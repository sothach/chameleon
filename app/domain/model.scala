import java.time.LocalDateTime

import model.Finish.{Finish, Glossy, Matte}
import play.api.libs.json._
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
                 dateTime: LocalDateTime = LocalDateTime.now,
                 status: JobStatus.JobStatus = JobStatus.Created, version: Int = 0)
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
    val Glossy: Finish = Value(0)
    val Matte: Finish = Value(1)
    implicit val format: Format[model.Finish.Value] = new Format[Finish.Value] {
      override def writes(o: Finish.Value): JsValue = Json.toJson(o.id)
      override def reads(json: JsValue): JsResult[Finish.Value] = json.validate[Int].map(Value)
    }
    def of(id: Int): Finish = if(id == 0) {
      Glossy
    } else {
      Matte
    }
  }
  case class Color (color: Int, finish: Finish = Glossy) {
    require(color > 0, s"color code must be 1 of above ($color)")
    def matte: Color = copy(finish = Matte)
    def gloss: Color = copy(finish = Glossy)
  }
  object Color {
    implicit val format: Format[Color] = new Format[Color] {
      override def writes(o: Color): JsValue = Json.toJson(o.color)
      override def reads(json: JsValue): JsResult[Color] = json.validate[Int].map(o => Color(o))
    }
  }
  case class Batch(colors: Array[Color]) {
    val mapped: Map[Int,Int] = colors.map { color =>
      color.color -> color.finish.id
    }.toMap
  }
  object Batch {
    implicit val format: Format[Batch] = new Format[Batch] {
      override def writes(o: Batch): JsValue = {
        val array = o.colors.length +: o.colors.flatMap(c => Array(c.color, c.finish.id))
        Json.toJson(array)
      }

      override def reads(json: JsValue): JsResult[Batch] = {
        val batch = json.as[Array[Int]]
        require(batch.length >= 3)
        val items = batch.drop(1).sliding(2,2).map { pair =>
          assert(pair.length == 2, "color/finish spec requires two values")
          Color(pair.head, Finish.of(pair.last))
        }.toArray
        JsSuccess(Batch(items))
      }
    }
  }

  case class JobSpecification(demands: Array[Batch]) {
    require(demands.length > 0, s"must be at least one demand (${demands.length})")
    val customers: Int = demands.length
    val colors: Int = demands.flatMap(_.colors.map(_.color)).distinct.length
  }

  object JobSpecification {
    private def readJson(json: JsValue) = {
      val demands = (json \ "demands").as[Array[Array[Int]]]
      val batches = demands.map { batch =>
        require(batch.length == (batch.head * 2) + 1, s"batch specification valid (${batch.mkString(",")})")
        val items = batch.drop(1).sliding(2,2).map { pair =>
          require(pair.length == 2, "color/finish spec requires two values")
          Color(pair.head, Finish.of(pair.last))
        }.toArray
        Batch(items)
      }
      JsSuccess(JobSpecification(batches))
    }
    private def writeJson(o: JobSpecification) = {
      val demands = o.demands.map { batch =>
        val count = batch.colors.length
        val colorSpecs = batch.colors.flatMap { color =>
          Array(color.color, color.finish.id)
        }
        count +: colorSpecs
      }
      val json = Json.toJson(demands)
      Json.parse(
        s"""{"colors":${o.colors},"customers":${o.customers},"demands":$json}""")
    }

  // """{"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
  implicit val format: Format[JobSpecification] = new Format[JobSpecification] {
    override def writes(o: JobSpecification): JsValue = writeJson(o)
    override def reads(json: JsValue): JsResult[JobSpecification] = readJson(json)
  }
  }

  case class MixSolution(batch: Batch)
  object MixSolution {
    def withColors(colors: Seq[Color]): MixSolution = {
      MixSolution(Batch(colors.toArray))
    }
    implicit val format: Format[MixSolution] = Json.format
  }

}
