package forex.services.rates

import forex.domain.Rate
import Errors._

trait Algebra[F[_]] {
  def getAllRates(): F[OneFrameError Either List[Rate]]
}
