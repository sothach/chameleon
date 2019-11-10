package api

import controllers.LegacyController
import model.{Batch, Finish, JobSpecification, Paint}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test.Helpers._
import play.api.test._

class LegacyControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "LegacyController GET" should {
    val url = """/v1/?input={"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
    val jobSpec = JobSpecification(1,Array(
      Batch(Array(Paint(1,Finish.Matte))),
      Batch(Array(Paint(1,Finish.Glossy)))
    ))

    "answer with the expected value from a new instance of controller" in {
      val controller = new LegacyController(stubControllerComponents())
      val response = controller.request(jobSpec).apply(FakeRequest(GET, url))

      status(response) mustBe OK
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include ("IMPOSSIBLE")
    }

    "answer with the expected value from the application" in {
      val controller = inject[LegacyController]
      val response = controller.request(jobSpec).apply(FakeRequest(GET, url))

      status(response) mustBe OK
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include ("IMPOSSIBLE")
    }

    "answer with the expected value from the router" in {
      val request = FakeRequest(GET, url)
      val response = route(app, request).get

      status(response) mustBe OK
      contentType(response) mustBe Some("text/plain")
      contentAsString(response) must include ("IMPOSSIBLE")
    }
  }
}
