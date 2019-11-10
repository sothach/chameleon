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

  case class Job(userEmail: String, request: JobSpecification, result: Option[MixSolution] = None,
                 created: LocalDateTime = LocalDateTime.now,
                 status: JobStatus.JobStatus = JobStatus.Created, jobId: Long = 0, version: Int = 0) {
    def withSolution(solution: MixSolution): Job = copy(result=Some(solution), status=JobStatus.Completed)
  }
  object Job {
   implicit val format: Format[Job] = Json.format
  }

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
  case class Paint(color: Int, finish: Finish = Glossy) {
    require(color > 0, s"color code must be 1 of above ($color)")
    def matte: Paint = copy(finish = Matte)
    def gloss: Paint = copy(finish = Glossy)
  }
  object Paint {
    implicit val format: Format[Paint] = new Format[Paint] {
      override def writes(o: Paint): JsValue = Json.toJson(o.color)
      override def reads(json: JsValue): JsResult[Paint] = json.validate[Int].map(o => Paint(o))
    }
  }
  case class Batch(paints: Array[Paint]) {
    val mapped: Map[Int,Int] = paints.map { paint =>
      paint.color -> paint.finish.id
    }.toMap
  }
  object Batch {
    implicit val format: Format[Batch] = new Format[Batch] {
      override def writes(o: Batch): JsValue = {
        val array = o.paints.length +: o.paints.flatMap(c => Array(c.color, c.finish.id))
        Json.toJson(array)
      }

      override def reads(json: JsValue): JsResult[Batch] = {
        val batch = json.as[Array[Int]]
        require(batch.length >= 3)
        val items = batch.drop(1).sliding(2,2).map { pair =>
          assert(pair.length == 2, "color/finish spec requires two values")
          Paint(pair.head, Finish.of(pair.last))
        }.toArray
        JsSuccess(Batch(items))
      }
    }
  }

  case class JobSpecification(colors: Int, demands: Array[Batch]) {
    require(demands.length > 0, s"must be at least one demand (${demands.length})")
    val nbCustomers: Int = demands.length
    val allColors: Array[Int] = demands.flatMap(_.paints.map(_.color)).distinct
  }

  object JobSpecification {
    private def readJson(json: JsValue) = {
      val colors = (json \ "colors").as[Int]
      val demands = (json \ "demands").as[Array[Array[Int]]]
      val batches = demands.map { batch =>
        require(batch.length == (batch.head * 2) + 1, s"batch specification valid (${batch.mkString(",")})")
        val items = batch.drop(1).sliding(2,2).map { pair =>
          require(pair.length == 2, "color/finish spec requires two values")
          Paint(pair.head, Finish.of(pair.last))
        }.toArray
        Batch(items)
      }
      JsSuccess(JobSpecification(colors,batches))
    }
    private def writeJson(o: JobSpecification) = {
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

  // """{"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}"""
  implicit val format: Format[JobSpecification] = new Format[JobSpecification] {
    override def writes(o: JobSpecification): JsValue = writeJson(o)
    override def reads(json: JsValue): JsResult[JobSpecification] = readJson(json)
  }
  }

  case class MixSolution(batch: Batch) {
    val legacyFormat: String = batch.paints.map(_.color).mkString(" ")
  }
  object MixSolution {
    def withPaints(colors: Seq[Paint]): MixSolution = {
      MixSolution(Batch(colors.toArray))
    }
    implicit val format: Format[MixSolution] = Json.format
  }

}
