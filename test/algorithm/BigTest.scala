package algorithm

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import algorithm.simple.OptimizerUsingPermutations
import com.typesafe.config.ConfigFactory
import fixtures.RequestGenerator
import model.Finish.{Glossy, Matte}
import model._
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.inject.DefaultApplicationLifecycle
import services.{JobService, MixService, RequestValidator}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success}

class BigTest extends FlatSpec with Matchers with OptionValues with MockitoSugar {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")
  implicit val ec: scala.concurrent.ExecutionContext = system.dispatcher
  private val subject = new OptimizerUsingPermutations
  info("Ensure the solution behaves well for large requests")

  "A quite large request (3000 - epsilon t-values)" should
    "be successfully optimized" in {
    val request = RequestGenerator.generateRequest(55, 54)
    val result = Await.result(subject.optimize(request), Duration.Inf)
    result.value.finishes.length should be(55)
  }

  "A large request (> 3000 t-values)" should
    "fail the optimizer's preconditions" in {
    val t = intercept[IllegalArgumentException] {
      val request = RequestGenerator.generateRequest(55, 55)
      Await.result(subject.optimize(request), Duration.Inf)
    }
    t.getMessage should include("sum of all t-values in request should not exceed 3000")
  }

  "pipeline" should "handle many requests" in {
    val planner = mock[OptimizerUsingPermutations]
    val jobService = mock[JobService]
    when(jobService.create(any[Job])) thenAnswer ((context: InvocationOnMock) => {
      val args = context.getArguments
      Future.successful(args(0).asInstanceOf[Job])
    })
    when(jobService.update(any[Job])) thenReturn (Future.successful(1))
    val configuration = new Configuration(ConfigFactory.parseMap(
      Map("mixer-service.process.timeout" -> "1s",
        "mixer-service.process.parallelism" -> "100",
        "mixer-service.limits.max-colors" -> "2000",
        "mixer-service.limits.max-customers" -> "2000",
        "mixer-service.limits.t-max" -> "3000"
      ).asJava))
    val lifecycle = new DefaultApplicationLifecycle
    val solution = Some(MixSolution(Seq(Glossy, Glossy, Matte, Glossy)))
    when(planner.optimize(any[JobSpecification])).thenReturn(Future.successful(solution))

    val subject = new MixService(planner, jobService,
      new RequestValidator(configuration), configuration, lifecycle)

    val user = EmailAddress("big@tester.org").get
    val jobs = RequestGenerator.generateSeries(1000,100,70) map { spec =>
      Job(user, spec)
    }
    //val result = Await.result(subject.seekSolutions(Source.fromIterator(() => jobs)), 2 seconds)
    val request = Job(user,RequestGenerator.generateRequest(2000, 2000))
    val result = Await.result(subject.seekSolutions(Source.single(request)), 2 seconds)
    result match {
      case seq if seq.nonEmpty =>
        println(s"#${seq.size} processed")
        seq foreach {
          case Success(mix) =>
            println(s"success: $mix")
          case Failure(t) =>
            println(s"failure: ${t.getMessage}")
        }
      case _ =>
        fail
    }
  }

}