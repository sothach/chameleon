package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import model.Finish.{Glossy, Matte}
import model.{JobSpecification, MixSolution}
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Configuration, Environment}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class MixServiceSpec extends FlatSpec with MockitoSugar with MustMatchers {
  private implicit val system: ActorSystem = ActorSystem.create("test-actor-system")

  "When an existing job is queried, it" should "be returned" in {
    val planner = mock[PaintShopPlanner]
    val configuration = mock[Configuration]
    val environment = mock[Environment]
    val solution = Some(MixSolution(Array(Glossy,Glossy,Matte,Glossy)))
    when(planner.solve(any[JobSpecification])).thenReturn(solution)

    val request = JobSpecification(1, 2, Array(Array(1,1,1),Array(1,1,0)))
    val subject = new MixService(planner, configuration, environment)

    val result = Await.result(subject.seekSolution(Source(immutable.Seq(request))), 2 seconds)
    result match {
      case solution if solution.nonEmpty =>
        println(solution)
      case _ =>
        fail
    }
  }

}
