package api

import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.ActorSystem
import algorithm.simple.OptimizerUsingPermutations
import com.typesafe.config.{Config, ConfigFactory}
import controllers.ApiController
import model.Finish.{Glossy, Matte}
import model.UserRole.{Customer, UserRole}
import model.{Batch, EmailAddress, Job, JobSpecification, Paint, UserRole}
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
import play.api.mvc.BodyParsers.Default
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, _}
import play.api.{Application, Configuration}
import security.{Authorization, JwtUtility}
import services.{ChronoService, JobService, MixService, RequestValidator}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class LegacyApiSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with MockitoSugar with MustMatchers {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")
  private implicit val ec: ExecutionContext = system.dispatcher
  private val chronoService = mock[ChronoService]
  private val testDateTime = LocalDateTime.parse("2019-11-12T12:33:34")
  when(chronoService.now) thenReturn(testDateTime)
  private val jobRepository = mock[JobRepository]
  when(jobRepository.create(any[Job])) thenAnswer ((context: InvocationOnMock) => {
    val args = context.getArguments
    Future.successful(args(0).asInstanceOf[Job])
  })
  when(jobRepository.update(any[Job])) thenReturn(Future.successful(1))
  private val jobService = new JobService(jobRepository,chronoService)
  private val applicationSecret = "secret"
  private val testEmail = EmailAddress("test@mail.org").value
  private val configuration = Configuration(ConfigFactory.load("test-application.conf"))
  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .loadConfig(configuration)
    .overrides(bind[JobRepository].toInstance(jobRepository),
               bind[JobService].toInstance(jobService),
               bind[ChronoService].toInstance(chronoService))
    .build()
  private val stubs = stubControllerComponents()
  val authority = new Authorization(chronoService, configuration, stubs.messagesApi, new Default(stubs.parsers))
  private val authToken = () => new JwtUtility(applicationSecret,() => chronoService.now)
    .createBearerToken(testEmail,Customer,testDateTime.plusMinutes(5)).value

  "LegacyController GET" should {

    "answer with the expected value from a new instance of controller" in {
      val jobSpec = JobSpecification.build(2, Batch(Paint(1, Glossy)), Batch(Paint(1, Glossy)))
      val mixer = new MixService(new OptimizerUsingPermutations(), jobService, new RequestValidator(configuration),
        configuration, new DefaultApplicationLifecycle)
      val controller = new ApiController(authority, mixer, jobService, stubControllerComponents())
      val response = controller.optimize(jobSpec).apply(
        FakeRequest(GET, "/v1/")
          .withHeaders(FakeHeaders(Map(
            "Host" -> "localhost",
            "Authorization" -> authToken(),
            "Accept-Language" -> "en",
            "Accept" -> "text/plain").toSeq)))

      status(response) mustBe OK
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include("0 0")
    }

    "answer with the expected Json value when Accept header appropriately set" in {
      val jobSpec = JobSpecification.build(2, Batch(Paint(1).gloss), Batch(Paint(1).gloss))
      val mixer = new MixService(new OptimizerUsingPermutations(), jobService, new RequestValidator(configuration),
        configuration, new DefaultApplicationLifecycle)
      val controller = new ApiController(authority, mixer, jobService, stubControllerComponents())
      val response = controller.optimize(jobSpec).apply(
        FakeRequest(GET, "/v1/")
          .withHeaders(FakeHeaders(Map(
            "Host" -> "localhost",
            "Authorization" -> authToken(),
            "Accept-Language" -> "en",
            "Accept" -> "application/json").toSeq)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must equal("""{"finishes":[0,0]}""")
    }

    "answer with the expected value from the application" in {
      val jobSpec = JobSpecification.build(1, Batch(Paint(1,Matte)), Batch(Paint(1,Glossy)))
      val controller = inject[ApiController]
      val response = controller.optimize(jobSpec).apply(
        FakeRequest(GET, "/v1/").withHeaders(FakeHeaders(Map(
          "Authorization" -> authToken()).toSeq)))

      status(response) mustBe UNPROCESSABLE_ENTITY
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include ("IMPOSSIBLE")
    }

    "answer with the expected value from the router" in {
      val url = """/v1/?input={"colors":1,"customers":2,"demands":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(GET, url)
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Authorization" -> authToken(),
          "Accept-Language" -> "en",
          "Accept" -> "text/plain").toSeq))

      val response = route(app, request).get

      status(response) mustBe UNPROCESSABLE_ENTITY
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include ("IMPOSSIBLE")
    }

    "return a client error if badly-formed request received" in {
      val url = """/v1/?input={"colors":1,"customers":2,demands":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(GET, url)
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Authorization" -> authToken(),
          "Accept-Language" -> "en",
          "Accept" -> "text/plain").toSeq))
      val response = route(app, request).get

      status(response) mustBe BAD_REQUEST
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must startWith ("com.fasterxml.jackson.core.JsonParseException: Unexpected character")
    }

    "returns a client error if the requested spec cannot be optimized" in {
      val url = """/v1/?input={"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
      val request = FakeRequest(GET, url)
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Authorization" -> authToken(),
          "Accept-Language" -> "en",
          "Accept" -> "text/plain").toSeq))

      val response = route(app, request).get

      status(response) mustBe UNPROCESSABLE_ENTITY
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must equal ("IMPOSSIBLE")
    }
  }

}
