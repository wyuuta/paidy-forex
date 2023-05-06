package forex.services.rates

object Errors {

  sealed trait OneFrameError
  object OneFrameError {
    final case class OneFrameLookupFailed(msg: String) extends OneFrameError
  }

}
