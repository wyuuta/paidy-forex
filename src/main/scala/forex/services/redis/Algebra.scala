package forex.services.redis

import forex.domain.Rate
import forex.services.redis.Errors.RedisError
import forex.services.redis.Errors.RedisError.RedisConnectionFailed

trait Algebra[F[_]] {

  def get(key: String): F[Either[RedisError, Rate]]

  def store(rates: List[Rate]): F[Either[RedisConnectionFailed, Boolean]]
}
