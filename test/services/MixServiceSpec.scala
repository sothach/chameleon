package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import algorithm.simple.OptimizerUsingPermutations
import com.typesafe.config.ConfigFactory
import model.Finish.{Glossy, Matte}
import model._
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.inject.DefaultApplicationLifecycle

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success}

class MixServiceSpec extends FlatSpec with MockitoSugar with MustMatchers with OptionValues {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")
  implicit def toAnswer[T](f: () => T): Answer[T] = (_: InvocationOnMock) => f()

  "When a mix batch is requested, it" should "be calculated by the service" in {
    val planner = mock[OptimizerUsingPermutations]
    val jobService = mock[JobService]
    when(jobService.create(any[Job])) thenAnswer ((context: InvocationOnMock) => {
      val args = context.getArguments
      Future.successful(args(0).asInstanceOf[Job])
    })
    when(jobService.update(any[Job])) thenReturn (Future.successful(1))
    val configuration = new Configuration(ConfigFactory.parseMap(
      Map("mixer-service.process.timeout" -> "1s",
        "mixer-service.process.parallelism" -> "1").asJava))
    val lifecycle = new DefaultApplicationLifecycle
    val solution = Some(MixSolution(Seq(Glossy, Glossy, Matte, Glossy)))
    when(planner.optimize(any[JobSpecification])).thenReturn(Future.successful(solution))

    val subject = new MixService(planner, jobService,
      new RequestValidator(configuration), configuration, lifecycle)

    val color1 = Paint(1)
    val request =
      Job(EmailAddress("test@mail.org").value, JobSpecification.build(1, Batch(color1.gloss), Batch(color1.matte)))

    val result = Await.result(subject.seekSolutions(Source(immutable.Seq(request))), 2 seconds)
    result match {
      case Seq(Success(job), _ @ _*) =>
        job.result.value.finishes must be(Seq(Glossy, Glossy, Matte, Glossy))
      case _=>
        fail
    }
  }

}
