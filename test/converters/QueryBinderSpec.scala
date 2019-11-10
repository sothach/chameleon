package converters

import conversions.Binders._
import model.{Batch, Finish, JobSpecification, Paint}
import org.scalatest.{EitherValues, FlatSpec, Matchers, OptionValues}

class QueryBinderSpec extends FlatSpec with Matchers with EitherValues with OptionValues {

  "A job request" should "be unbound as a query parameter" in {
    val batches = Array(
      Batch(Array(Paint(1,Finish.Matte))),
      Batch(Array(Paint(1,Finish.Glossy)))
    )
    val jobSpec = JobSpecification(1,batches)
    val result = requestBinder.unbind("input", jobSpec)
    val decoded = java.net.URLDecoder.decode(result, "UTF-8")
    decoded shouldBe """input={"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
  }

  "A valid job request parameter" should "bind" in {
    val input = """{"colors":5,"customers":3,"demands":[[1,1,1],[2,1,0,2,0],[1,5,0]]}"""
    val result = requestBinder.bind("input", Map("input" -> Seq(input)))
    val jobSpec = result.value.right.value
    jobSpec.colors shouldBe 5
    jobSpec.nbCustomers shouldBe 3
    jobSpec.demands.length shouldBe 3
  }

  "An invalid job request parameter" should "not bind" in {
    val result = requestBinder.bind("input", Map("noInput" -> Seq.empty))
    result.exists(_.isLeft) shouldBe true
  }

}
