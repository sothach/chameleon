package api

import controllers.ApiController
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

class ApiControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "ApiController GET" should {

    "answer with the expected value from a new instance of controller" in {
      val controller = new ApiController(stubControllerComponents())
      val index = controller.index().apply(FakeRequest(GET, "/"))

      status(index) mustBe OK
      contentType(index) mustBe Some("text/plain")
      contentAsString(index) must include ("Computer")
    }

    "answer with the expected value from the application" in {
      val controller = inject[ApiController]
      val index = controller.index().apply(FakeRequest(GET, "/"))

      status(index) mustBe OK
      contentType(index) mustBe Some("text/plain")
      contentAsString(index) must include ("Computer")
    }

    "answer with the expected value from the router" in {
      val request = FakeRequest(GET, "/")
      val index = route(app, request).get

      status(index) mustBe OK
      contentType(index) mustBe Some("text/plain")
      contentAsString(index) must include ("Computer")
    }
  }
}
