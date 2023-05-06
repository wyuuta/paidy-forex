package forex.services.redis

import cats.effect.{Concurrent, ContextShift}
import forex.config.RedisConfig

object Interpreters {

  def rateRedis[F[_]: Concurrent : ContextShift](config: RedisConfig): Algebra[F] = new RedisRateService[F](config)
}

