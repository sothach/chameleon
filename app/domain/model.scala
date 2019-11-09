import java.time.{LocalDate, LocalDateTime}

import model.Finish.Finish
import play.api.libs.json.{Format, JsResult, JsValue, Json}

package object model {

  object JobStatus extends Enumeration {
    type JobStatus = Value
    val Created, Active, Completed, Failed, Deleted = Value
    implicit val format: Format[model.JobStatus.Value] = new Format[JobStatus.Value] {
      override def writes(o: JobStatus.Value): JsValue = Json.toJson(o.toString)
      override def reads(json: JsValue): JsResult[JobStatus.Value] = json.validate[String].map(JobStatus.withName)
    }
  }

  case class Job(jobId: Long, userEmail: String, request: String,
                 dateTime: LocalDateTime, status: JobStatus.JobStatus, version: Int = 0)
  object Job {
   implicit val format: Format[Job] = Json.format
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
  object Finish extends Enumeration {
    type Finish = Value
    val Glossy, Matte = Value
    implicit val format: Format[model.Finish.Value] = new Format[Finish.Value] {
      override def writes(o: Finish.Value): JsValue = Json.toJson(o.toString)
      override def reads(json: JsValue): JsResult[Finish.Value] = json.validate[String].map(Finish.withName)
    }
  }

  case class JobSpecification(colors: Int, customers: Int, demands: Array[Array[Int]])
  object JobSpecification {
    implicit val format: Format[JobSpecification] = Json.format
  }
  // Case #2: 1 0 0 0 0
  case class MixSolution(batch: Array[Finish])

}
