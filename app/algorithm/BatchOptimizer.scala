package algorithm
import model.{JobSpecification, MixSolution}
import scala.concurrent.Future

trait BatchOptimizer {
  def optimize(jobSpec: JobSpecification): Future[Option[MixSolution]]
}
