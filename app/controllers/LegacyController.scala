package controllers

import akka.stream.scaladsl.Source
import javax.inject._
import model._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, _}
import services.MixService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class LegacyController @Inject()(mixer: MixService, cc: ControllerComponents)
                                (implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val logger = Logger(this.getClass)

  def request(spec: JobSpecification): Action[AnyContent] =
    Action.async { implicit request =>
      lazy val messages = messagesApi.preferred(request)
      lazy val impossible = messages("request.not.solvable")
      logger.debug(s"solve: $spec")
      val fakeUser = EmailAddress("faed@mail.org").get
      val source = Source.single(Job(fakeUser, spec))
      val response = singleResult(mixer.seekSolutions(source)) map {
        case Success(Job(_, _, Some(result), _, _, _, _)) =>
          renderResponse(result)
        case Success(_) =>
          UnprocessableEntity(impossible)
        case Failure(t: RequestError) =>
          logger.warn(s"RequestError: ${messages(t.key, t.params)}")
          BadRequest(messages(t.key, t.params))
        case Failure(t: ProcessingError) =>
          logger.warn(s"ProcessingError: ${messages(t.key, t.params)}")
          InternalServerError(messages(t.key, t.params))
        case Failure(t) =>
          logger.warn(s"Failure($t)")
          InternalServerError(t.getMessage)
        case _ =>
          UnprocessableEntity(impossible)
      }
      response.recover {
        case ex =>
          logger.warn(s"recover from ($ex)")
          InternalServerError
      }
    }

  private def singleResult(result: Future[Seq[Try[Job]]]) = result map { value =>
    value.headOption match {
      case Some(result) => result
      case None =>
        Failure(ProcessingError("processing.error.no-results"))
    }
  }

  private def renderResponse(solution: MixSolution)
                            (implicit request: Request[AnyContent]) = render {
    case Accepts.Json() =>
      import conversions.JsonFormatters.mixFormat
      Ok(Json.toJson(solution))
    case _ =>
      Ok(solution.legacyFormat)
  }

}

