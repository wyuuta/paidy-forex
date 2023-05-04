package forex.services.rates.interpreters

import cats.effect.{ConcurrentEffect, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxEitherId, toFunctorOps}
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.http.rates.Protocol.rateListDecoder
import forex.services.rates.Algebra
import forex.services.rates.Errors.Error.OneFrameLookupFailed
import forex.services.rates.Errors._
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

class OneFrameClient[F[_]: ConcurrentEffect](config: OneFrameConfig) extends Algebra[F] {

  private implicit val responseEntityDecoder: EntityDecoder[F, List[Rate]] = jsonOf[F, List[Rate]]
  private val httpClientResource : Resource[F, Client[F]] = BlazeClientBuilder[F](ExecutionContext.global).resource

  def getAllOneFrameRates(pair: Rate.Pair): F[Error Either List[Rate]] = {
    val uri = s"${config.baseUrl}/rates"

    val request = Request[F](
      method = Method.GET,
      uri = Uri.unsafeFromString(uri)
        .withQueryParam("pair", s"${pair.from}${pair.to}"),
      headers = Headers.of(Header("token", config.token))
    )

    httpClientResource.use(client => client.expect[List[Rate]](request))
      .map(response => response.asRight[Error])
      .handleError(_ => {
        OneFrameLookupFailed("Received error from OneFrame").asLeft[List[Rate]]
      })
  }

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
//    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error].pure[F]
    getAllOneFrameRates(pair)
      .map {
        case Right(value) => Right(value.last)
        case Left(value) => Left(value)
      }
  }

}
