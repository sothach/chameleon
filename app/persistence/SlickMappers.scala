package persistence
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}

import model.JobStatus.JobStatus
import model.{EmailAddress, JobSpecification, JobStatus, MixSolution}
import play.api.libs.json.Json
import slick.ast.BaseTypedType
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcType

import scala.util.Try

object SlickMappers {
  import conversions.JsonFormatters._

  implicit val statusMapper: JdbcType[JobStatus] with BaseTypedType[JobStatus] =
    MappedColumnType.base[JobStatus, String](e => e.toString,
      status => Try(JobStatus.withName(status)).getOrElse(
        throw new IllegalArgumentException(s"JobStatus '$status' doesn't exist")))

  implicit val jobSpecMapper: JdbcType[JobSpecification] with BaseTypedType[JobSpecification] =
    MappedColumnType.base[JobSpecification, String](
      jobSpec => Json.toJson(jobSpec).toString,
      s => Json.parse(s).validate[JobSpecification].getOrElse(
        throw new IllegalArgumentException(s"jobSpec $s could not be parsed")))

  implicit val solutionMapper: JdbcType[MixSolution] with BaseTypedType[MixSolution] =
    MappedColumnType.base[MixSolution, String](
      solution => Json.toJson(solution).toString,
      s => Json.parse(s).validate[MixSolution].getOrElse(
        throw new IllegalArgumentException(s"solution $s could not be parsed")))

  implicit val localDateTimeColumnType: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, Timestamp](
      d => Timestamp.from(d.toInstant(ZoneOffset.ofHours(0))),
      d => d.toLocalDateTime)

  implicit val emailMapper: JdbcType[EmailAddress] with BaseTypedType[EmailAddress] =
    MappedColumnType.base[EmailAddress, String](
      email => email.address,
      s => EmailAddress(s).getOrElse(
        throw new IllegalArgumentException(s"email $s could not be parsed")))

}
