package forex.services.redis

import cats.effect.{ContextShift, IO, Resource}
import dev.profunktor.redis4cats.RedisCommands
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.redis.Errors.RedisError.{RedisConnectionFailed, RedisEmpty}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{doReturn, doThrow, times, verify}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class RedisRateServiceTest extends AnyFlatSpec {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "get() method" should "return Rate if found" in {
    val client: RedisCommands[IO, String, String] = mock[RedisCommands[IO, String, String]]
    val clientResource: Resource[IO, RedisCommands[IO, String, String]] =
      Resource.make[IO, RedisCommands[IO, String, String]](IO.pure(client))(_ => IO.unit)

    val expectedRateString: String =
      s"""
         |{
         |    "from": "SGD",
         |    "to": "JPY",
         |    "price": 0.8699731881,
         |    "timestamp": "2023-05-07T17:47:33.896Z"
         |}
         |""".stripMargin
    val expectedRate: Rate = Rate(
      Currency.SGD,
      Currency.JPY,
      Price(BigDecimal.valueOf(0.8699731881)),
      Timestamp(OffsetDateTime.parse("2023-05-07T17:47:33.896Z"))
    )

    doReturn(IO.pure(Some(expectedRateString)))
      .when(client)
      .get(anyString())

    val result = new RedisRateService[IO](clientResource).get("key").unsafeRunSync();

    assert(result == Right(expectedRate))
  }

  it should "return RedisEmpty if not found" in {
    val client: RedisCommands[IO, String, String] = mock[RedisCommands[IO, String, String]]
    val clientResource: Resource[IO, RedisCommands[IO, String, String]] =
      Resource.make[IO, RedisCommands[IO, String, String]](IO.pure(client))(_ => IO.unit)

    doReturn(IO.pure(None))
      .when(client)
      .get(anyString())

    val result = new RedisRateService[IO](clientResource).get("key").unsafeRunSync();

    assert(result == Left(RedisEmpty()))
  }

  it should "return RedisConnectionFailed if caught any error" in {
    val client: RedisCommands[IO, String, String] = mock[RedisCommands[IO, String, String]]
    val clientResource: Resource[IO, RedisCommands[IO, String, String]] =
      Resource.make[IO, RedisCommands[IO, String, String]](IO.pure(client))(_ => IO.unit)

    doThrow(new IllegalArgumentException())
      .when(client)
      .get(anyString())

    val result = new RedisRateService[IO](clientResource).get("key").unsafeRunSync();

    assert(result == Left(RedisConnectionFailed("Redis connection failed")))
  }

  "store() method" should "store all rates" in {
    val client: RedisCommands[IO, String, String] = mock[RedisCommands[IO, String, String]]
    val clientResource: Resource[IO, RedisCommands[IO, String, String]] =
      Resource.make[IO, RedisCommands[IO, String, String]](IO.pure(client))(_ => IO.unit)

    val rates: List[Rate] = List(
      Rate(Currency.USD, Currency.JPY, Price(BigDecimal.valueOf(1000)), Timestamp(OffsetDateTime.now())),
      Rate(Currency.AUD, Currency.CHF, Price(BigDecimal.valueOf(0.145)), Timestamp(OffsetDateTime.now()))
    )

    doReturn(IO.pure(()))
      .when(client)
      .setEx(anyString(), anyString(), any[FiniteDuration])

    new RedisRateService[IO](clientResource).store(rates).unsafeRunSync();

    verify(client, times(rates.size)).setEx(anyString(), anyString(), any[FiniteDuration])
  }

  it should "return RedisConnectionFailed if caught any error" in {
    val client: RedisCommands[IO, String, String] = mock[RedisCommands[IO, String, String]]
    val clientResource: Resource[IO, RedisCommands[IO, String, String]] =
      Resource.make[IO, RedisCommands[IO, String, String]](IO.pure(client))(_ => IO.unit)

    val rates: List[Rate] = List(
      Rate(Currency.USD, Currency.JPY, Price(BigDecimal.valueOf(1000)), Timestamp(OffsetDateTime.now())),
      Rate(Currency.AUD, Currency.CHF, Price(BigDecimal.valueOf(0.145)), Timestamp(OffsetDateTime.now()))
    )

    doThrow(new IllegalArgumentException())
      .when(client)
      .get(anyString())

    val result = new RedisRateService[IO](clientResource).store(rates).unsafeRunSync();

    assert(result == Left(RedisConnectionFailed("Redis connection failed")))
  }
}
