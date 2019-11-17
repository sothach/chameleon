package services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import model.{Batch, EmailAddress, Job, JobSpecification, Paint, RequestError}
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class RequestValidatorSpec extends FlatSpec with MockitoSugar with MustMatchers with OptionValues {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")
  val configuration = new Configuration(ConfigFactory.parseMap(
    Map("mixer-service.process.timeout" -> "1s",
      "mixer-service.process.parallelism" -> "100",
      "mixer-service.limits.max-colors" -> "2000",
      "mixer-service.limits.max-customers" -> "2000",
      "mixer-service.limits.t-max" -> "3000"
    ).asJava))

  "When a correctly-formed job is validated, it" should "pass" in {
    val subject = new RequestValidator(configuration)
    val paint1 = Paint(1)
    val jobSpec = JobSpecification.build(1, Batch(paint1.matte, paint1.gloss))
    val job = Job(EmailAddress("test@mail.com").value, jobSpec, None, LocalDateTime.parse("2019-11-09T12:34:00"), jobId=10)
    subject.validate(job) must be(Success(job))
  }

  "A request that does not conform to the configured setting" should "fail" in {
    val subject = new RequestValidator(configuration)
    val paint1 = Paint(1)
    val batch = (1 to 2001) map (_ => Batch(paint1.matte, paint1.gloss))
    val jobSpec = JobSpecification(1, batch.toArray)
    val job = Job(EmailAddress("test@mail.com").value, jobSpec, None, LocalDateTime.parse("2019-11-09T12:34:00"), jobId=10)
    subject.validate(job) match {
      case Success(_) =>
        fail
      case Failure(RequestError(key,args)) =>
        key must be("request.error.nb-customers")
        args must be(Array("2001","2000"))
      case Failure(_) =>
        fail
    }
  }

}
