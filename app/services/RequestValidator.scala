package services

import javax.inject.Inject
import model.{Job, RequestError}
import play.api.Configuration

import scala.util.{Failure, Try}

class RequestValidator @Inject()(configuration: Configuration) {
  private val maxColors = configuration.getOptional[Int]("mixer-service.limits.max-colors").getOrElse(100)
  private val maxCustomers = configuration.getOptional[Int]("mixer-service.limits.max-customers").getOrElse(100)
  private val tMax = configuration.getOptional[Int]("mixer-service.limits.t-max").getOrElse(100)

  def validate(job: Job): Try[Job] = {
    def check(value: Int, limit: Int, key: String) = {
      if (!(1 to limit).contains(value)) {
        Some((key, Array(value, limit).map(_.toString)))
      } else {
        None
      }
    }
    lazy val tVals = job.request.demands.flatMap(_.paints).length
    check(job.request.colors, maxColors, "request.error.nb-colors")
      .orElse(check(job.request.nbCustomers, maxCustomers, "request.error.nb-customers"))
      .orElse(check(tVals, tMax, "request.error.nb-t-values"))
      .map(error => Failure(RequestError(error._1, error._2)))
      .getOrElse(Try(job))
  }
}