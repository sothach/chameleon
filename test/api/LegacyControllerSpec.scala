package api

import akka.actor.ActorSystem
import algorithm.simple.OptimizerUsingPermutations
import com.typesafe.config.{Config, ConfigFactory}
import controllers.LegacyController
import model.Finish.{Glossy, Matte}
import model.{Batch, Job, JobSpecification, Paint}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.MustMatchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import persistence.JobRepository
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{DefaultApplicationLifecycle, bind}
import play.api.test.Helpers._
import play.api.test._
import play.api.{Application, Configuration}
import services.{ChronoService, JobService, MixService, RequestValidator}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class LegacyControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with MockitoSugar with MustMatchers {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")
  private implicit val ec: ExecutionContext = system.dispatcher
  private val chronoService = mock[ChronoService]
  private val jobRepository = mock[JobRepository]
  when(jobRepository.create(any[Job])) thenAnswer ((context: InvocationOnMock) => {
    val args = context.getArguments
    Future.successful(args(0).asInstanceOf[Job])
  })
  when(jobRepository.update(any[Job])) thenReturn(Future.successful(1))
  private val jobService = new JobService(jobRepository,chronoService)

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .loadConfig(configureApp)
    .overrides(bind[JobRepository].toInstance(jobRepository),
               bind[JobService].toInstance(jobService),
               bind[ChronoService].toInstance(chronoService))
    .build()

  "LegacyController GET" should {

    "answer with the expected value from a new instance of controller" in {
      val jobSpec = JobSpecification.build(2, Batch(Paint(1, Glossy)), Batch(Paint(1, Glossy)))
      val mixer = new MixService(new OptimizerUsingPermutations(), jobService, new RequestValidator(configureApp),
        configureApp, new DefaultApplicationLifecycle)
      val controller = new LegacyController(mixer, stubControllerComponents())
      val response = controller.request(jobSpec).apply(
        FakeRequest(GET, "/v1/")
          .withHeaders(FakeHeaders(Map(
            "Host" -> "localhost",
            "Accept-Language" -> "en",
            "Accept" -> "text/plain").toSeq)))

      status(response) mustBe OK
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include("0 0")
    }

    "answer with the expected Json value when Accept header appropriately set" in {
      val jobSpec = JobSpecification.build(2, Batch(Paint(1, Glossy)), Batch(Paint(1, Glossy)))
      val mixer = new MixService(new OptimizerUsingPermutations(), jobService, new RequestValidator(configureApp),
        configureApp, new DefaultApplicationLifecycle)
      val controller = new LegacyController(mixer, stubControllerComponents())
      val response = controller.request(jobSpec).apply(
        FakeRequest(GET, "/v1/")
          .withHeaders(FakeHeaders(Map(
            "Host" -> "localhost",
            "Accept-Language" -> "en",
            "Accept" -> "application/json").toSeq)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must equal("""{"finishes":[0,0]}""")
    }

    "answer with the expected value from the application" in {
      val jobSpec = JobSpecification.build(1, Batch(Paint(1,Matte)), Batch(Paint(1,Glossy)))
      val controller = inject[LegacyController]
      val response = controller.request(jobSpec).apply(FakeRequest(GET, "/v1/"))

      status(response) mustBe UNPROCESSABLE_ENTITY
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include ("IMPOSSIBLE")
    }

    "answer with the expected value from the router" in {
      val url = """/v1/?input={"colors":1,"customers":2,"demands":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(GET, url)
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Accept-Language" -> "en",
          "Accept" -> "text/plain").toSeq))

      val response = route(app, request).get

      status(response) mustBe UNPROCESSABLE_ENTITY
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include ("IMPOSSIBLE")
    }

    "return a client error if badly formed request receivedr" in {
      val url = """/v1/?input={"colors":1,"customers":2,demands":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(GET, url)
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Accept-Language" -> "en",
          "Accept" -> "text/plain").toSeq))
      val response = route(app, request).get

      status(response) mustBe BAD_REQUEST
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must startWith ("Unexpected character")
    }

    "returns a client error if the requested spec cannot be optimized" in {
      val url = """/v1/?input={"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
      val request = FakeRequest(GET, url)
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Accept-Language" -> "en",
          "Accept" -> "text/plain").toSeq))
      val response = route(app, request).get

      status(response) mustBe UNPROCESSABLE_ENTITY
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must equal ("IMPOSSIBLE")
    }
  }

  private val applicationSecret = "secret"

  private val configureApp: Configuration = {
    val baseCfg = Configuration(ConfigFactory.load("test-application.conf"))
    val extraConfig: Config = ConfigFactory.parseMap(Map(
      "mixer-service.process.timeout" -> "2s",
      "mixer-service.process.parallelism" -> "2",
      "play.http.secret.key" -> applicationSecret
    ).asJava)
    baseCfg ++ Configuration(extraConfig)
  }

}
