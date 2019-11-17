package conversions

import model.{EmailAddress, JobSpecification}
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
            Left("missing/invalid job spec")
        }
        Some(result)
      }

      override def unbind(key: String, jobSpec: JobSpecification): String = {
        binder.unbind("input", Json.toJson(jobSpec).toString)
      }
    }

  implicit def emailBinder(implicit binder: QueryStringBindable[String]): QueryStringBindable[Option[EmailAddress]] =
    new QueryStringBindable[Option[EmailAddress]] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Option[EmailAddress]]] = {
        val maybeEmail = (for {
          param <- binder.bind("email", params) if param.isRight
          email <- param.toOption.map(EmailAddress(_))
        } yield email).flatten
        Some(Right(maybeEmail))
      }

      override def unbind(key: String, email: Option[EmailAddress]): String =
        email.map(e => binder.unbind("email", e.address)).getOrElse("")
    }

  private def parseSpec(value: Either[String, String]): Either[String, JobSpecification] = value match {
    case Right(json) => try {
      Json.parse(json).validate[JobSpecification].asEither.left.map(_.toString())
    } catch { case ex: Exception =>
        Left(ex.getMessage)
    }
    case Left(t) => Left(s"missing/invalid query parameter $t")
  }
}
