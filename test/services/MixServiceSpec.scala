package services

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import model.Finish.{Glossy, Matte}
import model.{Batch, Color, JobSpecification, MixSolution}
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

  "When a mix batch is requested, it" should "be calculated by the service" in {
    val planner = mock[PaintShopPlanner]
    val configuration = mock[Configuration]
    val environment = mock[Environment]
    val solution = Some(MixSolution.withColors(Seq(Color(1,Glossy),Color(1,Glossy),Color(1,Matte),Color(1,Glossy))))
    when(planner.solve(any[JobSpecification])).thenReturn(solution)

    val color1 = Color(1)
    val request = JobSpecification(
      Array(
        Batch(Array(color1.gloss)),
        Batch(Array(color1.matte))
      ))
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
