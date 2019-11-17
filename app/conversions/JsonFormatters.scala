package conversions

import model._
import play.api.libs.json._

object JsonFormatters {

  implicit object emailFormat extends Format[EmailAddress] {
    def writes(o: EmailAddress): JsValue = Json.toJson(o.address)
    def reads(json: JsValue): JsResult[EmailAddress] =
      json.validate[String].map(EmailAddress(_)) match {
        case JsSuccess(Some(email), path) =>
          JsSuccess(email, path)
        case JsSuccess(_, _) | JsError(_) =>
          JsError("unable to parse email address")
     }
  }

  implicit object jobStatusFormat extends Format[JobStatus.Value] {
    def writes(o: JobStatus.Value): JsValue = Json.toJson(o.toString)
    def reads(json: JsValue): JsResult[JobStatus.Value] =
      json.validate[String].map(JobStatus.withName)
  }

  implicit object batchFormat extends Format[Batch] {
    def writes(o: Batch): JsValue =
      Json.toJson(o.paints.length +: o.paints.flatMap(c => Array(c.color, c.finish.id)))
    def reads(json: JsValue): JsResult[Batch] = {
      val batch = json.as[Array[Int]]
      require(batch.length >= 3)
      val items = batch.drop(1).sliding(2, 2).map { pair =>
        assert(pair.length == 2)
        Paint(pair.head, Finish(pair.last))
      }.toArray
      JsSuccess(Batch(items))
    }
  }

  implicit object finishFormat extends Format[Finish.Value] {
    def writes(o: Finish.Value): JsValue = Json.toJson(o.id)
    def reads(json: JsValue): JsResult[Finish.Value] = json.validate[Int].map {
      case 0 => Finish.Glossy
      case 1 => Finish.Matte
    }
  }

  implicit object jobSpecFormat extends Format[JobSpecification] {
    def reads(json: JsValue): JsResult[JobSpecification] = {
      val mColors = (json \ "colors").validate[Int]
      val mDemands = (json \ "demands").validate[Array[Array[Int]]]
      (mColors, mDemands) match {
        case (JsSuccess(colors, _), JsSuccess(demands, _)) =>
          val batches = demands.map { batch =>
            require(batch.length == (batch.head * 2) + 1)
            val items = batch.drop(1).sliding(2, 2).map { pair =>
              require(pair.length == 2)
              Paint(pair.head, Finish(pair.last))
            }.toArray
            Batch(items)
          }
          JsSuccess(JobSpecification(colors, batches))
        case _ =>
          JsError()
      }
    }

    def writes(o: JobSpecification): JsValue = {
      val demands = o.demands.map { batch =>
        val count = batch.paints.length
        val colorSpecs = batch.paints.flatMap { color =>
          Array(color.color, color.finish.id)
        }
        count +: colorSpecs
      }
      val json = Json.toJson(demands)
      Json.parse(
        s"""{"colors":${o.colors},"customers":${o.nbCustomers},"demands":$json}""")
    }
  }

  implicit object userRoleFormat extends Format[UserRole.Value] {
    def writes(o: UserRole.Value): JsValue = Json.toJson(o.toString)
    def reads(json: JsValue): JsResult[UserRole.Value] =
      json.validate[String].map(UserRole.withName)
  }

  implicit val userFormat: Format[User] = Json.format[User]

  implicit val mixFormat: OFormat[MixSolution] = Json.format[MixSolution]
  implicit val jobFormat: OFormat[Job] = Json.format[Job]
}
