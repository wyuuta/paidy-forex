package forex.services.redis

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxEitherId, toFunctorOps, toTraverseOps}
import dev.profunktor.redis4cats.effect.Log.Stdout._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import forex.config.RedisConfig
import forex.domain.Rate
import forex.http.rates.Protocol.{rateEncoder, redisRateDecoder}
import forex.services.redis.Errors.RedisError
import forex.services.redis.Errors.RedisError.{RedisConnectionFailed, RedisEmpty, RedisParseFailed}
import io.circe.parser
import io.circe.syntax.EncoderOps

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class RedisRateService[F[_] : Concurrent : ContextShift](config: RedisConfig) extends Algebra[F] {

  val redisClient: Resource[F, RedisCommands[F, String, String]] = Redis[F].utf8(s"${config.host}:${config.port}")
  val expiredDuration: FiniteDuration = 5.minutes

  override def get(key: String): F[Either[RedisError, Rate]] = redisClient
    .use(client => client.get(key))
    .map {
      case Some(value) => parser.decode[Rate](value) match {
        case Right(value) => value.asRight[RedisError]
        case Left(err) => RedisParseFailed(err.getMessage).asLeft[Rate]
      }
      case None => RedisEmpty().asLeft[Rate]
    }
    .handleError(err => RedisConnectionFailed(err.getMessage).asLeft[Rate])


  override def store(rates: List[Rate]): F[Either[RedisConnectionFailed, Boolean]] = redisClient
    .use(client => {
      rates.traverse(rate => client.setEx(s"${rate.from}${rate.to}", rate.asJson.toString(), expiredDuration))
    })
    .map(_ => true.asRight[RedisConnectionFailed])
    .handleError(err => RedisConnectionFailed(err.getMessage).asLeft[Boolean])
}
