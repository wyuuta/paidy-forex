package forex.services.redis

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxEitherId, toFlatMapOps, toFunctorOps, toTraverseOps}
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import forex.config.RedisConfig
import forex.domain.Rate
import forex.http.rates.Protocol.{rateEncoder, redisRateDecoder}
import forex.services.redis.Errors.RedisError
import forex.services.redis.Errors.RedisError.{RedisConnectionFailed, RedisEmpty}
import io.circe.parser
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class RedisRateService[F[_] : Concurrent : ContextShift](config: RedisConfig) extends Algebra[F] {

  val logger: F[SelfAwareStructuredLogger[F]] = Slf4jLogger.create[F]
  val redisClient: Resource[F, RedisCommands[F, String, String]] = Redis[F].utf8(s"${config.host}:${config.port}")
  val expiredDuration: FiniteDuration = 4.minutes

  override def get(key: String): F[Either[RedisError, Rate]] = redisClient
    .use(client => client.get(key))
    .map {
      case Some(value) => parser.decode[Rate](value).toOption.get.asRight[RedisError]
      case None => RedisEmpty().asLeft[Rate]
    }
    .handleErrorWith(err => {
      logger.flatMap(logger => logger.error(err)("Redis connection failure"))
        .map(_ => RedisConnectionFailed("Redis connection failure").asLeft[Rate])
    })

  override def store(rates: List[Rate]): F[Either[RedisConnectionFailed, Boolean]] = redisClient
    .use(client => {
      rates.traverse(rate => client.setEx(s"${rate.from}${rate.to}", rate.asJson.toString(), expiredDuration))
    })
    .map(_ => true.asRight[RedisConnectionFailed])
    .handleErrorWith(err => {
      logger.flatMap(logger => logger.error(err)("Redis connection failure"))
        .map(_ => RedisConnectionFailed("Redis connection failure").asLeft[Boolean])
    })
}
