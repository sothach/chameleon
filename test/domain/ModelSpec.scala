package domain

import java.time.LocalDateTime

import model.{Job, JobStatus}
import org.scalatest.{FlatSpec, Matchers}

class ModelSpec extends FlatSpec with Matchers {

  "A Job" should "be defined" in {
    val subject = Job(10, "test@mail.com", "", LocalDateTime.now(), JobStatus.Created)
    subject.version should be(0)
  }

}
