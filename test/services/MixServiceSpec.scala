package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import algorithm.simple.OptimizerUsingPermutations
import com.typesafe.config.ConfigFactory
import model.Finish.{Glossy, Matte}
import model._
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.{AsyncFlatSpec, MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.inject.DefaultApplicationLifecycle

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.Future
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success}

class MixServiceSpec extends AsyncFlatSpec with MockitoSugar with MustMatchers with OptionValues {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")
  private val paint1 = Paint(1)

  val configuration = new Configuration(ConfigFactory.parseMap(
    Map("mixer-service.process.timeout" -> "1s",
      "mixer-service.process.parallelism" -> "100",
      "mixer-service.limits.max-colors" -> "2000",
      "mixer-service.limits.max-customers" -> "2000",
      "mixer-service.limits.t-max" -> "3000"
    ).asJava))
  val lifecycle = new DefaultApplicationLifecycle

  "When a mix batch is requested, it" should "be calculated by the service" in {
    val planner = mock[OptimizerUsingPermutations]
    val jobService = mock[JobService]
    when(jobService.create(any[Job])).thenReturn(Future.successful(testJob()))
    when(jobService.update(any[Job])) thenReturn (Future.successful(1))
    val solution = Some(MixSolution(Seq(Glossy, Glossy, Matte, Glossy)))
    when(planner.optimize(any[JobSpecification])).thenReturn(Future.successful(solution))

    val subject = new MixService(planner, jobService,
      new RequestValidator(configuration), configuration, lifecycle)

    subject.seekSolutions(Source(immutable.Seq(testJob()))) map {
      case Seq(Success(job), _ @ _*) =>
        job.result.value.finishes must be(Seq(Glossy, Glossy, Matte, Glossy))
      case _=>
        fail
    }
  }

  "When an error occurs in the processing pipeline" should "be handled" in {
    val planner = mock[OptimizerUsingPermutations]
    when(planner.optimize(any[JobSpecification])).thenReturn(Future.failed(
      new IllegalArgumentException("sum of all t-values in request should not exceed 3000")))
    val jobService = mock[JobService]
    when(jobService.create(any[Job])).thenReturn(Future.successful(testJob()))
    val subject = new MixService(planner, jobService,
      new RequestValidator(configuration), configuration, lifecycle)

    subject.seekSolutions(Source(immutable.Seq(testJob()))) map { response =>
      response must be(empty)
    }
  }

  "When an error occurs in the processing pipeline" should "be passed thru" in {
    val planner = mock[OptimizerUsingPermutations]
    when(planner.optimize(any[JobSpecification])).thenReturn(Future.failed(
      new IllegalArgumentException("sum of all t-values in request should not exceed 3000")))
    val jobService = mock[JobService]
    when(jobService.create(any[Job])).thenReturn(Future.successful(testJob()))
    val subject = new MixService(planner, jobService,
      new RequestValidator(configuration), configuration, lifecycle)
    val batch = (1 to 2001) map (_ => Batch(paint1.matte, paint1.gloss))
    val badJob = testJob().copy(request=JobSpecification(1, batch.toArray))
    subject.seekSolutions(Source.single(badJob)) map {
      case Seq(Failure(RequestError(key, args))) =>
        key must be("request.error.nb-customers")
        args must be(Seq("2001", "2000"))
      case _ =>
        fail
    }

  }

  private val testJob = () =>
    Job(EmailAddress("test@mail.org").value, JobSpecification.build(1, Batch(paint1.gloss), Batch(paint1.matte)))

}
