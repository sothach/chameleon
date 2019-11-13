package conversions

import model.JobSpecification
import play.api.libs.json.Json
import play.api.mvc.QueryStringBindable

object Binders {
  import JsonFormatters._

  implicit def requestBinder(implicit binder: QueryStringBindable[String]): QueryStringBindable[JobSpecification] =
    new QueryStringBindable[JobSpecification] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, JobSpecification]] = {
        val result = binder.bind("input", params) match {
          case Some(value) if value.isRight =>
            parseSpec(value)
          case _ =>
            Left("Unable to bind input")
        }
        Some(result)
      }

      override def unbind(key: String, jobSpec: JobSpecification): String = {
        binder.unbind("input", Json.toJson(jobSpec).toString)
      }
    }

  private def parseSpec(value: Either[String, String]): Either[String, JobSpecification] = value match {
    case Right(json) => try {
      Json.parse(json).validate[JobSpecification].asEither.left.map(_.toString())
    } catch { case ex: Exception =>
        Left(ex.getMessage)
    }
    case Left(t) => Left(s"Unable to bind input $t")
  }
}
