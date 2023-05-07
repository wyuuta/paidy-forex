package forex.programs.rates

import cats.effect.Concurrent
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, toFlatMapOps, toFunctorOps}
import forex.domain._
import forex.programs.rates.Errors._
import forex.services.redis.Errors.RedisError.RedisEmpty
import forex.services.{RatesService, RedisService}

class Program[F[_]: Concurrent](
    ratesService: RatesService[F],
    redisService: RedisService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = redisService
    .get(s"${request.from}${request.to}")
    .flatMap {
      case Right(value) => value.asRight[Error].pure[F]
      case Left(_ : RedisEmpty) => getAndStoreRate(request)
      case Left(err) => toProgramError(err).asLeft[Rate].pure[F]
    }

  private def getAndStoreRate(request: Protocol.GetRatesRequest): F[Either[Error, Rate]] =
    ratesService.getAllRates()
      .flatMap {
        case Right(rates) => redisService.store(rates)
          .map {
            case Right(_) => rates.find(rate => rate.from == request.from && rate.to == request.to).get.asRight[Error]
            case Left(error) => toProgramError(error).asLeft[Rate]
          }
        case Left(error) => toProgramError(error).asLeft[Rate].pure[F]
      }
}

object Program {

  def apply[F[_]: Concurrent](
      ratesService: RatesService[F],
      redisService: RedisService[F]
  ): Algebra[F] = new Program[F](ratesService, redisService)
}
