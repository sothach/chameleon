package services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import model.{Job, JobStatus}
import org.scalatest.{AsyncFlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import org.mockito.Matchers._
import scala.concurrent.duration._
import persistence.JobRepository

import scala.concurrent.{Await, Future}

class JobServiceSpec extends AsyncFlatSpec with MockitoSugar with MustMatchers {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")

  "When an existing job is queried, it" should "be returned" in {
    val jobRepo = mock[JobRepository]
    val expected = Job(10, "test@mail.com", "", LocalDateTime.now(), JobStatus.Created)
    when(jobRepo.findById(any[Long])).thenReturn(Future.successful(Some(expected)))
    val subject = new JobService(jobRepo)
    val result = Await.result(subject.findById(10), 2 seconds)
    result match {
      case Some(job) =>
        job must be(expected)
      case _ =>
        fail
    }
  }

}
