package persistence

import java.time.LocalDateTime

import akka.actor.ActorSystem
import com.google.inject.Singleton
import javax.inject.Inject
import model.JobStatus.JobStatus
import model.{EmailAddress, Job, JobSpecification, JobStatus, MixSolution}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.Effect
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContextExecutor, Future}

@Singleton
class JobRepository @Inject()(implicit system: ActorSystem,
                              protected val dbConfigProvider: DatabaseConfigProvider)
                                                  extends HasDatabaseConfigProvider[JdbcProfile] {
  import SlickMappers._
  private implicit val ec: ExecutionContextExecutor = system.dispatchers.lookup("persistence-context")
  private val logger = Logger(this.getClass)

  class JobTable(tag: Tag) extends Table[Job](tag, "job") {
    def jobId     = column[Long]      ("job_id", O.PrimaryKey, O.AutoInc)
    def userEmail = column[EmailAddress] ("user_email")
    def request   = column[JobSpecification]    ("request")
    def result    = column[Option[MixSolution]] ("result")
    def created   = column[LocalDateTime]  ("created")
    def status    = column[JobStatus] ("status")
    def version   = column[Int]       ("version")

    def * = (userEmail, request, result, created, status, jobId, version) <> ((Job.apply _).tupled, Job.unapply)
  }

  val jobs = TableQuery[JobTable]
  type JobStreamQuery = Query[Job, JobTable#TableElementType, Seq]

  def createTable(): DBIOAction[Unit, NoStream, Effect.Schema] = jobs.schema.createIfNotExists

  def findAll: Future[Seq[Job]] = db.run(jobs.result)

  def findById(id: Long): Future[Option[Job]] =
    db.run(findByIdQuery(id))

  def create(newJob: Job): Future[Job] = db.run(
    jobs returning jobs.map(_.jobId) into ((job, id) => job.copy(jobId = id)) += newJob)

  def update(job: Job): Future[Int] = {
    val action = for {
      Some(existing) <- findByIdQuery(job.jobId)
      result <- if (job.version == existing.version) {
        jobs.insertOrUpdate(job.copy(version = existing.version + 1))
      } else {
        logger.warn(s"Job.update: optimistic lock failed: jobId=${job.jobId} db version ${existing.version}, not ${job.version}")
        DBIO.failed(new RuntimeException(s"Concurrent update failure"))
      }
    } yield result
    db.run(action.transactionally)
  }

  def findByUserEmail(userEmail: EmailAddress): Future[Seq[Job]] = {
    val q = jobs
      .filter(_.userEmail === userEmail)
      .filter(_.status =!= JobStatus.Deleted)
    db.run(q.result)
  }

  private def findByIdQuery(jobId: Long): DBIO[Option[Job]] =
    jobs
      .filter(_.jobId === jobId)
      .result.headOption
}
