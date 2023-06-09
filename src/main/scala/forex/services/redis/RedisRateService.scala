package forex.services.redis

import cats.effect.{Concurrent, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxEitherId, toFlatMapOps, toFunctorOps, toTraverseOps}
import dev.profunktor.redis4cats.RedisCommands
import forex.domain.Rate
import forex.http.rates.Protocol.{rateEncoder, redisRateDecoder}
import forex.services.redis.Errors.RedisError
import forex.services.redis.Errors.RedisError.{RedisConnectionFailed, RedisEmpty}
import io.circe.parser
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class RedisRateService[F[_] : Concurrent](redisClientResource: Resource[F, RedisCommands[F, String, String]])
  extends Algebra[F] {

  private val logger: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]

  private val expiredDuration: FiniteDuration = 4.minutes
  private val connectionFailedErrorMessage = "Redis connection failed"

  override def get(key: String): F[Either[RedisError, Rate]] = redisClientResource
    .use(client => client.get(key))
    .map {
      case Some(value) => parser.decode[Rate](value).toOption.get.asRight[RedisError]
      case None => RedisEmpty().asLeft[Rate]
    }
    .handleErrorWith(err => {
      logger.flatMap(logger => logger.error(err)(connectionFailedErrorMessage))
        .map(_ => RedisConnectionFailed(connectionFailedErrorMessage).asLeft[Rate])
    })

  override def store(rates: List[Rate]): F[Either[RedisConnectionFailed, Boolean]] = redisClientResource
    .use(client => {
      rates.traverse(rate => client.setEx(s"${rate.from}${rate.to}", rate.asJson.toString(), expiredDuration))
    })
    .map(_ => true.asRight[RedisConnectionFailed])
    .handleErrorWith(err => {
      logger.flatMap(logger => logger.error(err)(connectionFailedErrorMessage))
        .map(_ => RedisConnectionFailed(connectionFailedErrorMessage).asLeft[Boolean])
    })

}
