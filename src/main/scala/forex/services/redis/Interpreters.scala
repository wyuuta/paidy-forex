package forex.services.redis

import cats.effect.{Concurrent, Resource}
import dev.profunktor.redis4cats.RedisCommands

object Interpreters {

  def rateRedis[F[_]: Concurrent](redisClientResource: Resource[F, RedisCommands[F, String, String]]): Algebra[F] =
    new RedisRateService[F](redisClientResource)
}

