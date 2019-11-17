package services

import akka.actor.ActorSystem
import com.google.inject.Inject
import model.{EmailAddress, Job, ProcessingError}
import persistence.JobRepository

import scala.concurrent.{ExecutionContextExecutor, Future}

class JobService @Inject()(jobRepo: JobRepository, chronoService: ChronoService)(implicit system: ActorSystem) {
  private implicit val ec: ExecutionContextExecutor = system.dispatchers.lookup("service-context")

  def create(job: Job): Future[Job] = jobRepo.create(job.copy(created=chronoService.now))
    .transform(identity,t => ProcessingError("processing.error.storage", t.getMessage))

  def findAll: Future[Seq[Job]] = jobRepo.findAll
    .transform(identity,t => ProcessingError("processing.error.query", t.getMessage))

  def findById(jobId: Int): Future[Option[Job]] = jobRepo.findById(jobId)
    .transform(identity,t => ProcessingError("processing.error.query", t.getMessage))

  def findByUserEmail(userEmail: EmailAddress): Future[Seq[Job]] = jobRepo.findByUserEmail(userEmail)
    .transform(identity,t => ProcessingError("processing.error.query", t.getMessage))

  def update(job: Job): Future[Int] = jobRepo.update(job)
    .transform(identity,t => ProcessingError("processing.error.update", t.getMessage))

}
