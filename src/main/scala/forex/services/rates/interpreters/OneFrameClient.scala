package forex.services.rates.interpreters

import cats.effect.{ConcurrentEffect, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxEitherId, toFunctorOps}
import forex.config.OneFrameConfig
import forex.domain.{Currency, Rate}
import forex.http.rates.Protocol.rateListDecoder
import forex.services.rates.Algebra
import forex.services.rates.Errors.OneFrameError.OneFrameLookupFailed
import forex.services.rates.Errors._
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class OneFrameClient[F[_]: ConcurrentEffect](config: OneFrameConfig) extends Algebra[F] {

  private implicit val responseEntityDecoder: EntityDecoder[F, List[Rate]] = jsonOf[F, List[Rate]]
  private val httpClientResource : Resource[F, Client[F]] = BlazeClientBuilder[F](ExecutionContext.global)
    .withIdleTimeout(1.minutes)
    .resource

  private val currencies: List[Currency] = List(
      Currency.AUD,
      Currency.CAD,
      Currency.CHF,
      Currency.EUR,
      Currency.GBP,
      Currency.NZD,
      Currency.JPY,
      Currency.SGD,
      Currency.USD
  )
  private val currencyParams = currencies
    .combinations(2)
    .flatMap(_.permutations)
    .collect(currencyPair => s"pair=${currencyPair.head}${currencyPair.last}")
    .toList
    .mkString("&")

  override def getAllRates(): F[OneFrameError Either List[Rate]] = {
    val uri = s"${config.baseUrl}/rates?${currencyParams}"

    val request = Request[F](
      method = Method.GET,
      uri = Uri.fromString(uri).toOption.get,
      headers = Headers.of(Header("token", config.token))
    )

    httpClientResource.use(client => client.expect[List[Rate]](request))
      .map(response => response.asRight[OneFrameError])
      .handleError(err => OneFrameLookupFailed(err.getMessage).asLeft[List[Rate]])
  }

}
