import java.time.LocalDateTime

import model.UserRole.UserRole

package object model {
  import model.Finish.{Finish, Glossy, Matte}

  object JobStatus extends Enumeration {
    type JobStatus = Value
    val Created, Active, Completed, Failed = Value
  }

  case class Job(userEmail: EmailAddress, request: JobSpecification, result: Option[MixSolution] = None,
                 created: LocalDateTime = LocalDateTime.now,
                 status: JobStatus.JobStatus = JobStatus.Created, jobId: Long = 0, version: Int = 0) {
    def withSolution(solution: MixSolution): Job = copy(result=Some(solution), status=JobStatus.Completed)
  }

  object Finish extends Enumeration {
    type Finish = Value
    val Glossy: Finish = Value(0)
    val Matte: Finish = Value(1)
  }
  case class Paint(color: Int, finish: Finish = Glossy) {
    require(color > 0, s"color code must be >= 1 (not $color)")
    def matte: Paint = copy(finish = Matte)
    def gloss: Paint = copy(finish = Glossy)
  }

  case class Batch(paints: Array[Paint]) extends AnyVal
  object Batch {
    def apply(paints: Paint*): Batch = Batch(paints.toArray)
  }

  case class JobSpecification(colors: Int, demands: Array[Batch]) {
    require(demands.length > 0, s"must be at least one demand (${demands.length})")
    val nbCustomers: Int = demands.length
    val allColors: Array[Int] = demands.flatMap(_.paints.map(_.color)).distinct
    override val toString: String =
      s"JobSpecification(colors=#$colors, demands=${demands.flatMap(_.paints).mkString(",")})"
  }

  object JobSpecification {
    def build(colors: Int, demands: Batch*): JobSpecification =
      JobSpecification(colors, demands.toArray)
  }

  case class MixSolution(finishes: Seq[Finish]) {
    override val toString: String = s"MixSolution(${finishes.mkString(",")})"
    val legacyFormat: String = finishes.map(_.id).mkString(" ")
  }
  object MixSolution {
    def of(finish: Finish*): MixSolution = MixSolution(finish)
  }

  case class EmailAddress(address: String)
  object EmailAddress {
    final private val emailPattern =
      """^\s*([a-z0-9.!#$%&’'*+/=?^_`{|}~-]+)@([a-z0-9-]+(?:\.[a-z0-9-]+)*)\s*$""".r
    def apply(email: String): Option[EmailAddress] = email.toLowerCase.replaceAll(" ","") match {
      case emailPattern(name, domain) => Some(new EmailAddress(s"$name@$domain"))
      case _ => None
    }
  }

  object UserRole extends Enumeration {
    type UserRole = Value
    val Customer, Admin = Value
    def of(name: String): Option[UserRole] =
      values.find(_.toString == name)
  }

  case class User(email: EmailAddress, role: UserRole = UserRole.Customer)

  case class ProcessingError(key: String, params: Array[String] = Array.empty) extends RuntimeException {
    override val getMessage: String = s"ProcessingError($key: {${params.mkString(",")}}"
  }
  object ProcessingError {
    def apply(key: String, params: String*): ProcessingError = ProcessingError(key, params.toArray)
  }
  case class RequestError(key: String, params: Array[String] = Array.empty) extends RuntimeException {
    override val getMessage: String = s"RequestError($key: {${params.mkString(",")}}"
  }
  object RequestError {
    def apply(key: String, params: String*): RequestError = RequestError(key, params.toArray)
  }
}
