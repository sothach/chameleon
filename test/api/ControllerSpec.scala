package api

import java.time.LocalDateTime

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import conversions.JsonFormatters._
import fixtures.RequestGenerator
import model.UserRole.{Customer, UserRole}
import model._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.postgresql.util.{PSQLException, PSQLState}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import persistence.JobRepository
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import play.api.{Application, Configuration}
import security.JwtUtility
import services.{ChronoService, JobService}

import scala.concurrent.Future
import scala.language.postfixOps

class ControllerSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite
       with MockitoSugar with BeforeAndAfterAll {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")

  private val testDateTime = LocalDateTime.parse("2019-11-12T12:33:34")
  val chronoService: ChronoService = new ChronoService {
    override def now: LocalDateTime =testDateTime
  }

  private val jobRepository = mock[JobRepository]
  when(jobRepository.create(any[Job])) thenAnswer ((context: InvocationOnMock) => {
    val args = context.getArguments
    Future.successful(args(0).asInstanceOf[Job])
  })
  when(jobRepository.update(any[Job])) thenReturn Future.successful(1)
  private val failedFuture = Future.failed(
    new PSQLException("database connection failure", PSQLState.IO_ERROR))
  when(jobRepository.create(any[Job])).thenReturn(failedFuture)

  private val jobService = new JobService(jobRepository,chronoService)

  private val authToken = (email: String, role: UserRole, expMins: Int) =>
    new JwtUtility("secret", () => chronoService.now)
      .createBearerToken(EmailAddress(email).value, role,
        chronoService.now.plusMinutes(expMins)).value

  private val configuration = Configuration(ConfigFactory.load("test-application.conf"))
  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .loadConfig(configuration)
      .overrides(bind[JobRepository].toInstance(jobRepository),
        bind[JobService].toInstance(jobService),
        bind[ChronoService].toInstance(chronoService))
      .build()
  }

  "When a request that exceeds the sizing parameters is posted, it" should {
    "return an appropriate bad request error message" in {
      val json = Json.toJson(RequestGenerator.generateRequest(55, 55))
      val request = FakeRequest(POST, "/api/v2/jobs/request")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "application/json",
          "Accept" -> "application/json",
          "Authorization" -> authToken("user@mail.org",Customer,5)
        ).toSeq)).withBody(json.toString())

      val response = route(app, request).get

      status(response) mustBe BAD_REQUEST
      contentAsString(response) must equal ("# t-values (3025) should be in range 1 .. 3000")
    }
  }

  "When the processing of a request fails" should {
    "return an appropriate internal server error message" in {
      val json = """{"colors":2,"customers":2,"demands":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(POST, "/api/v2/jobs/request")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "application/json",
          "Accept" -> "text/plain",
          "Authorization" -> authToken("test1@mail.org",Customer,5)
        ).toSeq)).withBody(json)

      val response = route(app, request).get

      status(response) mustBe INTERNAL_SERVER_ERROR
      contentAsString(response) must equal ("error storing: database connection failure")
    }
  }


}
