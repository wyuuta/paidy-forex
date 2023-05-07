package forex.services.rates

import cats.effect.{ContextShift, IO, Resource}
import forex.config.OneFrameConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.rates.Errors.OneFrameError.OneFrameLookupFailed
import forex.services.rates.interpreters.OneFrameService
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Request}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doReturn, doThrow}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext

class OneFrameServiceTest extends AnyFlatSpec{

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val config: OneFrameConfig = OneFrameConfig("baseUrl", "token")

  "get() method" should "return List of Rates" in {
    val client: Client[IO] = mock[Client[IO]]
    val clientResource: Resource[IO, Client[IO]] = Resource.make[IO, Client[IO]](IO.pure(client))(_ => IO.unit)

    val rates: List[Rate] = List(
      Rate(Currency.USD, Currency.JPY, Price(BigDecimal.valueOf(1000)), Timestamp(OffsetDateTime.now())),
      Rate(Currency.AUD, Currency.CHF, Price(BigDecimal.valueOf(0.145)), Timestamp(OffsetDateTime.now()))
    )

    doReturn(IO.pure(rates))
      .when(client)
      .expect[List[Rate]](any[Request[IO]])(any[EntityDecoder[IO, List[Rate]]])

    val result = new OneFrameService[IO](config, clientResource).getAllRates().unsafeRunSync()
    assert(result == Right(rates))
  }

  it should "return OneFrameLookupFailed error if the client throw any error" in {
    val client: Client[IO] = mock[Client[IO]]
    val clientResource: Resource[IO, Client[IO]] = Resource.make[IO, Client[IO]](IO.pure(client))(_ => IO.unit)

    doThrow(new IllegalArgumentException())
      .when(client)
      .expect[List[Rate]](any[Request[IO]])(any[EntityDecoder[IO, List[Rate]]])

    val result = new OneFrameService[IO](config, clientResource).getAllRates().unsafeRunSync()
    assert(result == Left(OneFrameLookupFailed("OneFrame request failure")))
  }
}
