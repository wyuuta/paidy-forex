package forex.programs.rates

import cats.effect.{ContextShift, IO}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.programs.rates.Errors.Error.RateLookupFailed
import forex.programs.rates.Protocol.GetRatesRequest
import forex.services.rates.Errors.OneFrameError.OneFrameLookupFailed
import forex.services.redis.Errors.RedisError.{RedisConnectionFailed, RedisEmpty}
import forex.services.{RatesService, RedisService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, never, verify}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext

class ProgramTest extends AnyFlatSpec {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "get() method" should "immediately return Rate if found in cache" in {
    val ratesService = mock[RatesService[IO]]
    val redisService = mock[RedisService[IO]]
    val rate = Rate(Currency.USD, Currency.JPY, Price(BigDecimal.valueOf(1000)), Timestamp(OffsetDateTime.now()))
    val request = GetRatesRequest(rate.from, rate.to)

    doReturn(IO.pure(Right(rate)))
      .when(redisService)
      .get(s"${request.from}${request.to}")

    val result = new Program[IO](ratesService, redisService)
      .get(request)
      .unsafeRunSync()

    assert(result == Right(rate))

    verify(ratesService, never()).getAllRates()
    verify(redisService, never()).store(any[List[Rate]])
  }

  it should "get rates and cache if previously not found in cache" in {
    val ratesService = mock[RatesService[IO]]
    val redisService = mock[RedisService[IO]]
    val rates: List[Rate] = List(
      Rate(Currency.USD, Currency.JPY, Price(BigDecimal.valueOf(1000)), Timestamp(OffsetDateTime.now())),
      Rate(Currency.AUD, Currency.CHF, Price(BigDecimal.valueOf(0.145)), Timestamp(OffsetDateTime.now()))
    )
    val request = GetRatesRequest(rates.head.from, rates.head.to)

    doReturn(IO.pure(Left(RedisEmpty())))
      .when(redisService)
      .get(s"${request.from}${request.to}")

    doReturn(IO.pure(Right(rates)))
      .when(ratesService)
      .getAllRates()

    doReturn(IO.pure(Right(true)))
      .when(redisService)
      .store(rates)

    val result = new Program[IO](ratesService, redisService)
      .get(request)
      .unsafeRunSync()

    assert(result == Right(rates.head))
  }

  it should "return RateLookupFailed when error getting from redis" in {
    val ratesService = mock[RatesService[IO]]
    val redisService = mock[RedisService[IO]]
    val request = GetRatesRequest(Currency.USD, Currency.JPY)

    doReturn(IO.pure(Left(RedisConnectionFailed("Redis connection failed"))))
      .when(redisService)
      .get(s"${request.from}${request.to}")

    val result = new Program[IO](ratesService, redisService)
      .get(request)
      .unsafeRunSync()

    assert(result == Left(RateLookupFailed("Redis connection failed")))

    verify(ratesService, never()).getAllRates()
    verify(redisService, never()).store(any[List[Rate]])
  }

  it should "return RateLookupFailed when error getting rates from OneFrame" in {
    val ratesService = mock[RatesService[IO]]
    val redisService = mock[RedisService[IO]]
    val request = GetRatesRequest(Currency.USD, Currency.JPY)

    doReturn(IO.pure(Left(RedisEmpty())))
      .when(redisService)
      .get(s"${request.from}${request.to}")

    doReturn(IO.pure(Left(OneFrameLookupFailed("OneFrame error"))))
      .when(ratesService)
      .getAllRates()

    val result = new Program[IO](ratesService, redisService)
      .get(request)
      .unsafeRunSync()

    assert(result == Left(RateLookupFailed("OneFrame error")))

    verify(redisService, never()).store(any[List[Rate]])
  }

  it should "return RateLookupFailed when error storing to redis" in {
    val ratesService = mock[RatesService[IO]]
    val redisService = mock[RedisService[IO]]
    val request = GetRatesRequest(Currency.USD, Currency.JPY)

    doReturn(IO.pure(Left(RedisEmpty())))
      .when(redisService)
      .get(s"${request.from}${request.to}")

    doReturn(IO.pure(Right(List())))
      .when(ratesService)
      .getAllRates()

    doReturn(IO.pure(Left(RedisConnectionFailed("Redis connection failed"))))
      .when(redisService)
      .store(List())

    val result = new Program[IO](ratesService, redisService)
      .get(request)
      .unsafeRunSync()

    assert(result == Left(RateLookupFailed("Redis connection failed")))
  }
}
