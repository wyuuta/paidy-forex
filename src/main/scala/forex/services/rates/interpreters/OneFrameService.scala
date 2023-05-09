package forex.services.rates.interpreters

import cats.effect.{Concurrent, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxEitherId, toFlatMapOps, toFunctorOps}
import forex.config.OneFrameConfig
import forex.domain.{Currency, Rate}
import forex.http.rates.Protocol.rateListDecoder
import forex.services.rates.Algebra
import forex.services.rates.Errors.OneFrameError.OneFrameLookupFailed
import forex.services.rates.Errors._
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class OneFrameService[F[_]: Concurrent](config: OneFrameConfig,
                                        httpClientResource: Resource[F, Client[F]]) extends Algebra[F] {

  private implicit val responseEntityDecoder: EntityDecoder[F, List[Rate]] = jsonOf[F, List[Rate]]
  private val logger: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]

  private val oneFrameErrorMessage = "OneFrame request failure"

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
      .handleErrorWith(err => {
        logger.flatMap(logger => logger.error(err)(oneFrameErrorMessage))
          .map(_ => OneFrameLookupFailed(oneFrameErrorMessage).asLeft[List[Rate]])
      })
  }

}
