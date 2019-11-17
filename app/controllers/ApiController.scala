package controllers

import akka.stream.scaladsl.Source
import conversions.JsonFormatters._
import io.swagger.annotations.{ApiParam, ApiResponse, ApiResponses}
import javax.inject.{Inject, Singleton}
import model.UserRole.{Admin, Customer}
import model._
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import security.{Authorization, UserRequest}
import services.{JobService, MixService}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ApiController @Inject()(authority: Authorization,
                              mixerService: MixService,
                              jobService: JobService,
                              components: ControllerComponents)
                                extends AbstractController(components) {
  implicit val ec: ExecutionContext = components.executionContext
  val logger = Logger(this.getClass)
  logger.info("ApiController started")


  def listJobs(userEmail: Option[EmailAddress]): Action[AnyContent] =
    authority.async { implicit request =>
      val emailKey = request.user.role match {
        case Admin => userEmail
        case Customer => Some(request.user.email)
      }
      emailKey match {
        case Some(email) =>
          jobService.findByUserEmail(email) map (jobs => Ok(Json.toJson(jobs)))
        case None =>
          jobService.findAll map (jobs => Ok(Json.toJson(jobs)))
      }
    }

  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid User role"),
    new ApiResponse(code = 422, message = "Request not solvable")))
  def optimize(@ApiParam(value = "Job to be optimized") jobSpec: JobSpecification): Action[AnyContent] =
    authority.async { implicit request =>
      if (request.user.role == Customer) {
        processRequest(jobSpec)
      } else {
        BadRequest(getMessage("request.invalid.user")).future
      }
    }

  def processRequest: Action[AnyContent] =
    authority.async { implicit request =>
      parseJobSpec(request.body.asJson) match {
        case Some(jobSpec) if request.user.role == Customer =>
          processRequest(jobSpec)
        case Some(_) =>
          BadRequest(getMessage("request.invalid.user")).future
        case None =>
          BadRequest(getMessage("request.invalid.body")).future
      }
    }

  private def processRequest(jobSpec: JobSpecification)
                              (implicit request: UserRequest[AnyContent]) = {
      val source = Source.single(Job(request.user.email, jobSpec))
      val response = singleResult(mixerService.seekSolutions(source)) map {
        case Success(Job(_, _, Some(result), _, _, _, _)) =>
          renderResponse(result)
        case Success(_) =>
          UnprocessableEntity(getMessage("request.not.solvable"))
        case Failure(t: RequestError) =>
          logger.warn(s"RequestError: ${getMessage(t.key, t.params)}")
          BadRequest(getMessage(t.key, t.params))
      }
      response.recover {
        case t: ProcessingError =>
          InternalServerError(getMessage(t.key, t.params))
        case _ =>
          InternalServerError
      }
    }

  private def parseJobSpec(value: Option[JsValue]): Option[JobSpecification] =
    for {
      json <- value
      result <- json.validate[JobSpecification]
        .asEither.left.map(_.toString()).toOption
    } yield result

  private def singleResult(result: Future[Seq[Try[Job]]]) =
    result map { value =>
      value.headOption match {
        case Some(result) => result
        case None =>
          Failure(ProcessingError("processing.error.no-results"))
      }
    }

  private def renderResponse(solution: MixSolution)
              (implicit request: Request[AnyContent]) = render {
    case Accepts.Json() =>
      import conversions.JsonFormatters._
      Ok(Json.toJson(solution))
    case _ =>
      Ok(solution.legacyFormat)
  }

  private def getMessage(key: String, params: Array[String] = Array.empty)
              (implicit request: Request[AnyContent]): String = {
    val messages = messagesApi.preferred(request)
    messages(key,params : _*)
  }

  private implicit class FutureHelp[T](value: T) {
    def future: Future[T] = Future {
      value
    }
  }

}
