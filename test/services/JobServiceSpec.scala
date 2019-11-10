package services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import model.{Batch, Finish, Job, JobSpecification, Paint}
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import persistence.JobRepository

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class JobServiceSpec extends AsyncFlatSpec with MockitoSugar with MustMatchers {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")

  "When an existing job is queried, it" should "be returned" in {
    val jobRepo = mock[JobRepository]
    val jobSpec = JobSpecification(1, Array(
      Batch(Array(Paint(1, Finish.Matte))),
      Batch(Array(Paint(1, Finish.Glossy)))
    ))
    val job = Job("test@mail.com", jobSpec, None, LocalDateTime.parse("2019-11-09T12:34:00"), jobId=10)
    when(jobRepo.findById(any[Long])).thenReturn(Future.successful(Some(job)))
    val chronoService = mock[ChronoService]
    val subject = new JobService(jobRepo,chronoService)
    val result = Await.result(subject.findById(10), 2 seconds)
    result match {
      case Some(job) =>
        job.userEmail must be("test@mail.com")
        job.created.toString must be("2019-11-09T12:34")
        //job.request
      case _ =>
        fail
    }
  }

}
