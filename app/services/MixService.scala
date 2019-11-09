package services

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.util.Timeout
import javax.inject.{Inject, Singleton}
import model.JobSpecification
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.Duration

@Singleton
class MixService @Inject()(planner: PaintShopPlanner,
                           configuration: Configuration,
                           environment: Environment)(implicit system: ActorSystem) {
  private implicit val ec: ExecutionContextExecutor = system.dispatchers.lookup("service-context")
  private implicit val timeout: Timeout =
    Timeout(Duration(configuration.getMillis("mixer.process.timeout"), TimeUnit.MILLISECONDS))
  val logger = Logger(this.getClass)
  val decider: Supervision.Decider = {
    case e: IllegalArgumentException =>
      logger.warn(s"Assertion failure: ${e.getMessage}")
      Supervision.Resume
  }
  private val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  private implicit val materializer: ActorMaterializer = ActorMaterializer(materializerSettings)(system)

  private val solve = Flow[JobSpecification] map { elem =>
    planner.solve(elem)
  }

  def seekSolution(source: Source[JobSpecification,_]) =
    source.async via solve runWith Sink.seq

}
