package persistence
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}

import model.JobStatus.JobStatus
import model.{EmailAddress, JobSpecification, JobStatus, MixSolution}
import play.api.libs.json.Json
import slick.ast.BaseTypedType
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcType


object SlickMappers {
  import conversions.JsonFormatters._

  implicit val statusMapper: JdbcType[JobStatus] with BaseTypedType[JobStatus] =
    MappedColumnType.base[JobStatus, String](e => e.toString, s => JobStatus.withName(s))

  implicit val jobSpecMapper: JdbcType[JobSpecification] with BaseTypedType[JobSpecification] =
    MappedColumnType.base[JobSpecification, String](
      jobSpec => Json.toJson(jobSpec).toString,
      s => Json.parse(s).validate[JobSpecification].get)

  implicit val solutionMapper: JdbcType[MixSolution] with BaseTypedType[MixSolution] =
    MappedColumnType.base[MixSolution, String](
      jobSpec => Json.toJson(jobSpec).toString,
      s => Json.parse(s).validate[MixSolution].get)

  implicit val localDateTimeColumnType: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, Timestamp](
      d => Timestamp.from(d.toInstant(ZoneOffset.ofHours(0))),
      d => d.toLocalDateTime)

  implicit val emailMapper: JdbcType[EmailAddress] with BaseTypedType[EmailAddress] =
    MappedColumnType.base[EmailAddress, String](
      email => email.address,
      s => EmailAddress(s).get)

}
