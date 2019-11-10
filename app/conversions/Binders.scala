package conversions

import model.JobSpecification
import play.api.libs.json.Json
import play.api.mvc.QueryStringBindable

object Binders {

  implicit def requestBinder(implicit binder: QueryStringBindable[String]): QueryStringBindable[JobSpecification] =
    new QueryStringBindable[JobSpecification] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, JobSpecification]] = {
      val result = binder.bind("input", params) match {
        case Some(v) if v.isRight =>
          v.map(parseSpec)
        case _ =>
          Left("Unable to bind input")
      }
      Some(result)
    }

    override def unbind(key: String, jobSpec: JobSpecification): String = {
      binder.unbind("input", Json.toJson(jobSpec).toString)
    }
  }

  private def parseSpec(input: String): JobSpecification = {
    Json.parse(input).validate[JobSpecification].get
  }

}
