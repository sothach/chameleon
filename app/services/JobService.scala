package services

import akka.actor.ActorSystem
import com.google.inject.Inject
import model.{Job, JobStatus}
import persistence.JobRepository

import scala.concurrent.{ExecutionContextExecutor, Future}

class JobService @Inject()(jobRepo: JobRepository)(implicit system: ActorSystem) {
  private implicit val ec: ExecutionContextExecutor = system.dispatchers.lookup("service-context")

  def create(job: Job): Future[Job] = jobRepo.create(job)
  def findById(jobId: Int): Future[Option[Job]] = jobRepo.findById(jobId)
  def findByUserEmail(userEmail: String): Future[Seq[Job]] = jobRepo.findByUserEmail(userEmail)
  def update(job: Job): Future[Int] = jobRepo.update(job)
  def delete(job: Job): Future[Int] = update(job.copy(status = JobStatus.Deleted))

}
