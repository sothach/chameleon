package converters

import conversions.Binders._
import model.{Batch, JobSpecification, Paint}
import org.scalatest.{EitherValues, FlatSpec, Matchers, OptionValues}

class QueryBinderSpec extends FlatSpec with Matchers with EitherValues with OptionValues {

  "A job request" should "be unbound as a query parameter" in {
    val batches = Array(Batch(Paint(1).matte), Batch(Paint(1).gloss))
    val jobSpec = JobSpecification(1,batches)
    val result = requestBinder.unbind("input", jobSpec)
    val decoded = java.net.URLDecoder.decode(result, "UTF-8")
    decoded shouldBe """input={"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
  }

  "A valid job request parameter" should "bind" in {
    val input = """{"colors":5,"customers":3,"demands":[[1,1,1],[2,1,0,2,0],[1,5,0]]}"""
    val result = requestBinder.bind("input", Map("input" -> Seq(input)))
    val jobSpec = result.value
    jobSpec match {
      case Right(spec) =>
        spec.colors shouldBe 5
        spec.nbCustomers shouldBe 3
        spec.demands.length shouldBe 3
      case Left(t) => fail(t)
    }
  }

  "An invalid job request parameter" should "not bind" in {
    val result = requestBinder.bind("input", Map("noInput" -> Seq.empty))
    result.exists(_.isLeft) shouldBe true
  }

}
