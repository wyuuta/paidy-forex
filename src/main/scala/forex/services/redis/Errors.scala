package forex.services.redis

object Errors {

  sealed trait RedisError {
    def getMessage : String
  }

  object RedisError {
    final case class RedisEmpty() extends RedisError {
      override def getMessage: String = "Redis empty"
    }
    final case class RedisConnectionFailed(msg: String) extends RedisError {
      override def getMessage: String = msg
    }
  }
}
