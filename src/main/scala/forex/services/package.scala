package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  final val RatesServices = rates.Interpreters

  type RedisService[F[_]] = redis.Algebra[F]
  final val RedisRateServices = redis.Interpreters
}
