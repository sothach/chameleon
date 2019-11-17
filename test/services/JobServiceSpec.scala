package services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import model._
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.postgresql.util.{PSQLException, PSQLState}
import org.scalatest.{AsyncFlatSpec, MustMatchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import persistence.JobRepository

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class JobServiceSpec extends AsyncFlatSpec with MockitoSugar with MustMatchers with OptionValues {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")

  "When an existing job is queried, it" should "be returned" in {
    val jobRepo = mock[JobRepository]
    when(jobRepo.findById(any[Long])).thenReturn(Future.successful(Some(testJob)))
    val subject = new JobService(jobRepo,new ChronoService)
    subject.findById(10) map {
      case Some(job) =>
        job.userEmail.address must be("test@mail.com")
        job.created.toString must be("2019-11-09T12:34")
        job.request.allColors must be(Array(1))
      case _ =>
        fail
    }
  }

  "In the case of a repository store error, the service" should "recover with a meaningful error" in {
    val subject = new JobService(failingJobRepo(),new ChronoService)
    val t = intercept[ProcessingError] {
      Await.result(subject.create(testJob), 2 seconds)
    }
    t.getMessage must include("processing.error.storage")
  }

  "In the case of a repository findAll query error, the service" should "recover with a meaningful error" in {
    val subject = new JobService(failingJobRepo(),new ChronoService)
    val t = intercept[ProcessingError] {
      Await.result(subject.findAll, 2 seconds)
    }
    t.getMessage must include("processing.error.query")
  }

  "In the case of a repository findById query error, the service" should "recover with a meaningful error" in {
    val subject = new JobService(failingJobRepo(),new ChronoService)
    val t = intercept[ProcessingError] {
      Await.result(subject.findById(0), 2 seconds)
    }
    t.getMessage must include("processing.error.query")
  }

  "In the case of a repository findByUserEmail query error, the service" should "recover with a meaningful error" in {
    val subject = new JobService(failingJobRepo(),new ChronoService)
    val t = intercept[ProcessingError] {
      Await.result(subject.findByUserEmail(EmailAddress("test@mail.org").value), 2 seconds)
    }
    t.getMessage must include("processing.error.query")
  }

  "In the case of a repository update error, the service" should "recover with a meaningful error" in {
    val subject = new JobService(failingJobRepo(),new ChronoService)
    val t = intercept[ProcessingError] {
      Await.result(subject.update(testJob), 2 seconds)
    }
    t.getMessage must include("processing.error.update")
  }

  private val failingJobRepo = () => {
    val failedFuture = Future.failed(
      new PSQLException("database connection failure", PSQLState.IO_ERROR))
    val jobRepo = mock[JobRepository]
    when(jobRepo.create(any[Job])).thenReturn(failedFuture)
    when(jobRepo.update(any[Job])).thenReturn(failedFuture)
    when(jobRepo.findAll).thenReturn(failedFuture)
    when(jobRepo.findById(any[Long])).thenReturn(failedFuture)
    when(jobRepo.findByUserEmail(any[EmailAddress])).thenReturn(failedFuture)
    jobRepo
  }

  private val testJob = {
    val paint1 = Paint(1)
    val jobSpec = JobSpecification.build(1, Batch(paint1.matte, paint1.gloss))
    Job(EmailAddress("test@mail.com").value, jobSpec, None, LocalDateTime.parse("2019-11-09T12:34:00"), jobId=10)
  }

}
