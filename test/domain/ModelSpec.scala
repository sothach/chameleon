package domain

import model._
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsError, JsSuccess, Json}

class ModelSpec extends FlatSpec with Matchers {

  "A Job" should "be defined" in {
    val jobSpec = JobSpecification(1, Array(
      Batch(Array(Paint(1, Finish.Matte))),
      Batch(Array(Paint(1, Finish.Glossy)))
    ))
    val subject = Job("test@mail.com", jobSpec)
    subject.status should be(JobStatus.Created)
    subject.version should be(0)
  }

  /*
    {
    "colors": 1,
    "customers": 2,
    "demands": [
      [1, 1, 1],
      [1, 1, 0]
    ]
  }
 */

  "A JobSpecification" should "be serialized as per the API spec" in {
    val batches = Array(
      Batch(Array(Paint(1,Finish.Matte))),
      Batch(Array(Paint(1,Finish.Glossy)))
    )
    val subject = JobSpecification(1,batches)
    val result = Json.toJson(subject)
    result.toString() should be("""{"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}""")
    (result \ "colors").as[Int] should be(1)
    (result \ "customers").as[Int] should be(2)
    val demands = (result \ "demands").as[Array[Batch]]
    println(s"demands=$demands")
  }

  "A JobSpecification string" should "be de-serialized to a domain object" in {
    val input = """{"colors":5,"customers":3,"demands":[[1,1,1],[2,1,0,2,0],[1,5,0]]}"""
    Json.parse(input).validate[JobSpecification] match {
      case JsSuccess(spec, _) =>
        spec.colors should be(5)
        spec.nbCustomers should be(3)
        spec.demands.length should be(spec.nbCustomers)
      case JsError(errors) =>
        fail(errors.map(_._2).mkString(","))
    }
  }

}
