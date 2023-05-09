package forex

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import dev.profunktor.redis4cats.effect.Log.Stdout._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import org.http4s._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class Module[F[_]: ConcurrentEffect: ContextShift: Timer](config: ApplicationConfig) {

  private val ratesService: RatesService[F] = {
    val httpClientResource = BlazeClientBuilder[F](ExecutionContext.global)
      .withIdleTimeout(1.minutes)
      .resource
    RatesServices.oneFrame(config.oneFrame, httpClientResource)
  }
  private val redisService: RedisService[F] = {
    val redisClientResource: Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(s"${config.redis.host}:${config.redis.port}")
    RedisRateServices.rateRedis(redisClientResource)
  }

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService, redisService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = { http: HttpRoutes[F] =>
    AutoSlash(http)
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
