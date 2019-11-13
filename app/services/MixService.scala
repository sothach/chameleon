package services

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.util.Timeout
import algorithm.simple.OptimizerUsingPermutations
import javax.inject.{Inject, Singleton}
import model.{Job, JobStatus, RequestError}
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logger}

import scala.collection.immutable
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class MixService @Inject()(optimizer: OptimizerUsingPermutations, jobService: JobService, validator: RequestValidator,
                           configuration: Configuration, lifecycle: ApplicationLifecycle)(implicit system: ActorSystem) {
  private val logger = Logger(this.getClass)
  implicit val ec: ExecutionContextExecutor = system.dispatchers.lookup("service-context")
  private implicit val timeout: Timeout =
    Timeout(Duration(configuration.getMillis("mixer-service.process.timeout"), TimeUnit.MILLISECONDS))
  private val parallelism = configuration.getOptional[Int]("mixer-service.process.parallelism").getOrElse(1)

  val decider: Supervision.Decider = {
    case e: IllegalArgumentException =>
      logger.warn(s"Assertion failure: ${e.getMessage}")
      Supervision.Resume
    case t: Throwable =>
      logger.warn(s"Fatal error: ${t.getMessage}", t)
      Supervision.Stop
  }
  private val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  private implicit val materializer: ActorMaterializer = ActorMaterializer(materializerSettings)(system)
  lifecycle.addStopHook { () => Future.successful(materializer.shutdown())}

  private val verifyRequest = Flow[Job] map { job =>
    validator.validate(job)
  }

  private val start = Flow[Try[Job]].mapAsync(parallelism) {
    case Success(job) =>
      jobService.create(job) map { inserted =>
        logger.debug(s"new $inserted created")
        Try(inserted)
      }
    case t@Failure(_) =>
      Future.successful(t)
  }

  private val solve = Flow[Try[Job]].mapAsync(parallelism) {
    case Success(job) =>
      logger.debug(s"seeking solution for $job")
      optimizer.optimize(job.request) map { solution =>
        Try(job.copy(result = solution))
      }
    case t @ Failure(_) =>
      Future.successful(t)
  }

  private val finish = Flow[Try[Job]].mapAsync (parallelism) {
    case Success(job) =>
      def endStatus = job.result match {
        case Some(_) =>
          JobStatus.Completed
        case None =>
          JobStatus.Failed
      }
      logger.debug(s"updating $job with status: $endStatus")
      val update = job.copy(status = endStatus)
      jobService.update(update) map { _ =>
        Try(update)
      }
    case t @ Failure(_) => Future.successful(t)
  }

  def seekSolutions(source: Source[Job,_]): Future[Seq[Try[Job]]] =
    source.async via verifyRequest via start via solve via finish runWith Sink.seq

}
