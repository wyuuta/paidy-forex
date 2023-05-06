package forex.programs.rates

import forex.services.rates.Errors.{OneFrameError => RatesServiceError}
import forex.services.redis.Errors.RedisError

object Errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
  }

  def toProgramError(error: RedisError): Error = Error.RateLookupFailed(error.getMessage)
}
