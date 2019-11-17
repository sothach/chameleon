package domain

import model._
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

class ModelSpec extends FlatSpec with Matchers with OptionValues {
  import conversions.JsonFormatters._

  "A Job" should "be defined" in {
    val jobSpec = JobSpecification(1, Array(
      Batch(Paint(1, Finish.Matte)),
      Batch(Paint(1, Finish.Glossy))
    ))
    val subject = Job(EmailAddress("test@mail.com").value, jobSpec)
    subject.status should be(JobStatus.Created)
    subject.version should be(0)
  }

  "A JobSpecification" should "be serialized as per the API spec" in {
    val batches = Array(
      Batch(Paint(1,Finish.Matte)),
      Batch(Paint(1,Finish.Glossy))
    )
    val subject = JobSpecification(1,batches)
    val result = Json.toJson(subject)
    result.toString() should be("""{"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}""")
    (result \ "colors").as[Int] should be(1)
    (result \ "customers").as[Int] should be(2)
    (result \ "demands").validate[Array[Batch]] forall { batch =>
      batch.length == 1
    }
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

  "Paint definition" should "enforce it's constraints" in {
    val t = intercept[IllegalArgumentException] {
      JobSpecification(1, Array(Batch(Paint(0).matte), Batch(Paint(0).gloss)))
    }
    t.getMessage should include("color code must be >= 1 (not 0)")
  }

  "An invalid email address" should "not be constructed in" in {
    EmailAddress("(DELETE FROM user)") should be (empty)
  }

  "An invalid email address in a json object" should "not be constructed in" in {
    val json = JsString("(DELETE FROM user)")
    val result = json.validate[String].map(EmailAddress(_)) match {
      case JsSuccess(Some(email), path) =>
        JsSuccess(email, path)
      case JsSuccess(_, _) | JsError(_) =>
        JsError("unable to parse email address")
    }
    result should be(JsError("unable to parse email address"))
  }

  "A User object" should "be serialized and deserialized" in {
    val subject = User(EmailAddress("test@mail.org").value,UserRole.Customer)
    val json = Json.toJson(subject)
    Json.parse(json.toString).validate[User] should be(JsSuccess(subject))
  }

  "A Batch object" should "be serialized and deserialized" in {
    val paints = (1 to 10) map (i => Paint(i,Finish(i%2)))
    val subject = Batch(paints.toArray)
    val json = Json.toJson(subject)
    json.validate[Batch] match {
      case JsSuccess(batch,_) =>
        batch.paints.length should be(10)
      case JsError(e) =>
        fail(e.toString)
    }
  }

}
