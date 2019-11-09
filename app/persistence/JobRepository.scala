package persistence

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.ActorSystem
import com.google.inject.Singleton
import javax.inject.Inject
import model.{Job, JobStatus}
import model.JobStatus.JobStatus
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContextExecutor, Future}

@Singleton
class JobRepository @Inject()(implicit system: ActorSystem,
                              protected val dbConfigProvider: DatabaseConfigProvider)
                                                  extends HasDatabaseConfigProvider[JdbcProfile] {
  private implicit val ec: ExecutionContextExecutor = system.dispatchers.lookup("db-context")
  private val logger = Logger(this.getClass)
  implicit val statusMapper: JdbcType[JobStatus] with BaseTypedType[JobStatus] =
    MappedColumnType.base[JobStatus, String](e => e.toString, s => JobStatus.withName(s))
  implicit val localDateTimeColumnType: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, Timestamp](
      d => Timestamp.from(d.toInstant(ZoneOffset.ofHours(0))),
      d => d.toLocalDateTime)

  class JobTable(tag: Tag) extends Table[Job](tag, "job") {
    def jobId     = column[Long]      ("job_id", O.PrimaryKey)
    def userEmail = column[String]    ("user_email")
    def details   = column[String]    ("request")
    def dateTime  = column[LocalDateTime] ("created")
    def status    = column[JobStatus] ("status")
    def version   = column[Int]       ("version")

    def * = (jobId, userEmail, details, dateTime, status, version) <> ((Job.apply _).tupled, Job.unapply)
  }

  val jobs = TableQuery[JobTable]

  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] = jobs.schema.createIfNotExists

  def findById(id: Long): Future[Option[Job]] =
    db.run(findByIdQuery(id))

  def create(newJob: Job): Future[Job] =
    db.run(
      jobs returning jobs.map(_.jobId) into ((job, id) =>
        job.copy(jobId = id)) += newJob)

  def update(job: Job): Future[Int] = {
    val action = for {
      Some(existing) <- findByIdQuery(job.jobId)
      result <- if (job.version == existing.version) {
        jobs.insertOrUpdate(job.copy(version = existing.version + 1))
      } else {
        logger.warn(s"User.update: Optimistic lock failed: ${job.jobId}'s db version ${existing.version}, not ${job.version}")
        DBIO.failed(new RuntimeException(s"Concurrent update failure"))
      }
    } yield result
    db.run(action.transactionally)
  }

  def updateStatus(id: Long, status: JobStatus): Future[Int] = {
    val q = for { job <- jobs if job.jobId === id} yield job.status
    db.run(q.update(status))
  }

  def findByUserEmail(userEmail: String): Future[Seq[Job]] = {
    val q = jobs
      .filter(_.userEmail.toLowerCase === userEmail.trim.toLowerCase)
      .filter(_.status =!= JobStatus.Deleted)
    db.run(q.result)
  }

  private def findByIdQuery(jobId: Long): DBIO[Option[Job]] =
    jobs
      .filter(_.jobId === jobId)
      .result.headOption
}
