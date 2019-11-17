package api

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.typesafe.config.{Config, ConfigFactory}
import model.Finish.Glossy
import model.UserRole.{Admin, Customer, UserRole}
import model.{Batch, EmailAddress, Job, JobSpecification, JobStatus, MixSolution, Paint, UserRole}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
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
import services.ChronoService
import conversions.JsonFormatters._

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ApiSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite with ForAllTestContainer with BeforeAndAfterAll {
  private val system = ActorSystem.create("test-actor-system")
  implicit val materializer: ActorMaterializer =
    ActorMaterializer(ActorMaterializerSettings(system))(system)
  override val container = PostgreSQLContainer()
  val chronoService: ChronoService = new ChronoService {
    override def now: LocalDateTime = LocalDateTime.parse("2019-11-12T12:33:45")
  }
  private lazy val configuration = configureApp()
  private val applicationSecret = "secret"
  private val authToken = (email: String, role: UserRole, expMins: Int) => new JwtUtility(
    applicationSecret, () => chronoService.now)
      .createBearerToken(EmailAddress(email).value, role,
        chronoService.now.plusMinutes(expMins)).value

  override def fakeApplication(): Application = {
    container.start()
    new GuiceApplicationBuilder()
      .loadConfig(configuration)
      .overrides(bind[ChronoService].toInstance(chronoService))
      .build()
  }

  "GET /api/v2/jobs/list" should {
    "return current user's jobs" in {
      val userEmail = EmailAddress("test7@mail.org").value
      val request = FakeRequest(GET, "/api/v2/jobs/list")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Accept" -> "application/json",
          "Authorization" -> authToken(userEmail.address,Customer,5)
        ).toSeq))

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe OK
        response.body.contentType must contain ("application/json")
        response.body.isKnownEmpty mustBe false
        val result = Await.result(response.body.consumeData, 2 seconds)
        val json = Json.parse(result.utf8String)
        json.validate[List[Job]].asEither match {
          case Right(jobs) =>
            jobs.size must be(1)
            jobs.head.userEmail must be(userEmail)
          case _ => fail
        }
      }
    }
  }

  "GET /api/v2/jobs/list" should {
    "return all users' jobs if caller is Admin" in {
      val request = FakeRequest(GET, "/api/v2/jobs/list")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Accept" -> "application/json",
          "Authorization" -> authToken("admin@mail.org",Admin,5)
        ).toSeq))

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe OK
        response.body.contentType must contain ("application/json")
        response.body.isKnownEmpty mustBe false
        val result = Await.result(response.body.consumeData, 2 seconds)
        val json = Json.parse(result.utf8String)
        json.validate[List[Job]].asEither match {
          case Right(_ :: _) =>
          case _ => fail
        }
      }
    }
  }

  "GET /api/v2/jobs/list" should {
    "return another user's jobs if caller is Admin" in {
      val userEmail = EmailAddress("test5@mail.org").value
      val request = FakeRequest(GET, s"/api/v2/jobs/list?email=${userEmail.address}")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Accept" -> "application/json",
          "Authorization" -> authToken("admin@mail.org",Admin,5)
        ).toSeq))

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe OK
        response.body.contentType must contain ("application/json")
        response.body.isKnownEmpty mustBe false
        val result = Await.result(response.body.consumeData, 2 seconds)
        val json = Json.parse(result.utf8String)
        json.validate[List[Job]].asEither match {
          case Right(jobs) =>
            jobs.size must be(2)
            jobs forall (_.userEmail == userEmail)
          case _ => fail
        }
      }
    }
  }

  "valid color spec posted to the /api/v2/jobs/request endpoint" should {
    "be optimized and returned as Json" in {
      val json = """{"colors":2,"customers":2,"demands":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(POST, "/api/v2/jobs/request")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "application/json",
          "Accept" -> "application/json",
          "Authorization" -> authToken("test1@mail.org",Customer,5)
        ).toSeq)).withBody(json)

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe OK
        response.body.contentType must contain("application/json")
        response.body.isKnownEmpty mustBe false
        val result = Await.result(response.body.consumeData, 2 seconds)
        result.utf8String mustBe """{"finishes":[0,0]}"""
      }
    }
  }

  "badly-formed posted to the /api/v2/jobs/request endpoint" should {
    "not be processed" in {
      val json = """{"colors":2,"customers":2,"paints":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(POST, "/api/v2/jobs/request")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "application/json",
          "Accept" -> "application/json",
          "Authorization" -> authToken("test@mail.org",Customer,5)
        ).toSeq)).withBody(json)

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe BAD_REQUEST
      }
    }
  }

  "requests posted to the /api/v2/jobs/request endpoint by an admin" should {
    "not be processed" in {
      val json = """{"colors":2,"customers":2,"demands":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(POST, "/api/v2/jobs/request")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "application/json",
          "Accept" -> "application/json",
          "Authorization" -> authToken("admin@mail.org",Admin,5)
        ).toSeq)).withBody(json)

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe BAD_REQUEST
      }
    }
  }

  "badly-formed requests to the /v1 endpoint by an admin" should {
    "not be processed" in {
      val json = """{"colors":2,"customers":2,"demands":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(GET, s"/v1/?input=$json")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "application/json",
          "Accept" -> "application/json",
          "Authorization" -> authToken("admin@mail.org",Admin,5)
        ).toSeq)).withBody(json)

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe BAD_REQUEST
      }
    }
  }

  "badly-formed requests to the /api/v2/jobs/request endpoint" should {
    "not be processed" in {
      val json = """{"colors":2,"customers":2,"paints":[[1,1,0],[1,2,0]]}"""
      val request = FakeRequest(GET, s"/v1/?input=$json")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "application/json",
          "Accept" -> "application/json",
          "Authorization" -> authToken("user@mail.org",Customer,5)
        ).toSeq)).withBody(json)

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe BAD_REQUEST
      }
    }
  }


  override def beforeAll(): Unit = {
    val jobRepo = app.injector.instanceOf[JobRepository]
    val jobSpec = JobSpecification.build(2, Batch(Paint(1, Glossy)), Batch(Paint(1, Glossy)))
    val solution = MixSolution.of(Glossy,Glossy)
    Seq(
      ("test1@mail.org","Created"),
      ("test1@mail.org","Failed"),
      ("test2@mail.org","Active"),
      ("test2@mail.org","Created"),
      ("test3@mail.org","Created"),
      ("test3@mail.org","Created"),
      ("test3@mail.org","Created"),
      ("test3@mail.org","Active"),
      ("test4@mail.org","Created"),
      ("test5@mail.org","Failed"),
      ("test5@mail.org","Created"),
      ("test6@mail.org","Failed"),
      ("test7@mail.org","Created")
    ) foreach { case (email, status) =>
        val jobStatus = JobStatus.withName(status)
      val mix = if(jobStatus == JobStatus.Completed) Some(solution) else None
      val job = Job(EmailAddress(email).value, jobSpec, mix, chronoService.now, jobStatus)
      jobRepo.create(job)
    }
  }

  override def afterAll(): Unit = {
    container.stop
  }

  private def configureApp(): Configuration = {
    val baseCfg = Configuration(ConfigFactory.load("test-application.conf"))
    val extraConfig: Config = ConfigFactory.parseMap(Map(
      "slick.dbs.default.db.url" -> container.jdbcUrl,
      "slick.dbs.default.db.user" -> container.username,
      "slick.dbs.default.db.password" -> container.password,
      "slick.dbs.default.driver" -> "slick.driver.PostgresDriver$",
      "slick.dbs.default.db.driver" -> "org.postgresql.Driver",
      "slick.dbs.default.db.numThreads" -> "1",
      "slick.dbs.default.db.maxConnections" -> "10"
    ).asJava)
    baseCfg ++ Configuration(extraConfig)
  }

}
