package forex.services.rates

import cats.effect.{ConcurrentEffect, Resource}
import forex.config.OneFrameConfig
import forex.services.rates.interpreters._
import org.http4s.client.Client

object Interpreters {
  def oneFrame[F[_]: ConcurrentEffect](config: OneFrameConfig,
                                       httpClientResource: Resource[F, Client[F]]): Algebra[F] =
    new OneFrameService[F](config, httpClientResource)
}
