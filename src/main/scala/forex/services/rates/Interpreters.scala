package forex.services.rates

import cats.effect.ConcurrentEffect
import forex.config.OneFrameConfig
import forex.services.rates.interpreters._

object Interpreters {
  def oneFrame[F[_]: ConcurrentEffect](config: OneFrameConfig): Algebra[F] = new OneFrameClient[F](config)
}
