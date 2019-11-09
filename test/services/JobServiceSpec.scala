package services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import model.Job
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
    val job = Job(10, "test@mail.com",
      """{"colors":1,"customers":2,"demands":[[1,1,1],[1,1,0]]}""", LocalDateTime.parse("2019-11-09T12:34:00"))
    when(jobRepo.findById(any[Long])).thenReturn(Future.successful(Some(job)))
    val chronoService = mock[ChronoService]
    val subject = new JobService(jobRepo,chronoService)
    val result = Await.result(subject.findById(10), 2 seconds)
    result match {
      case Some(job) =>
        job.userEmail must be("test@mail.com")
        job.dateTime.toString must be("2019-11-09T12:34")
        //job.request
      case _ =>
        fail
    }
  }

}
