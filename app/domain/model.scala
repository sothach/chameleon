import java.time.{LocalDate, LocalDateTime}

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

}
