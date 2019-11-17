package controllers

import javax.inject.{Inject, Singleton}
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler, JsonHttpErrorHandler, PreferredMediaTypeHttpErrorHandler}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._

object plainTextHttpErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    Future.successful(Status(statusCode)(message))
  def onServerError(request: RequestHeader, t: Throwable): Future[Result] =
    Future.successful(InternalServerError(t.getMessage))
}

@Singleton
class ErrorHandler @Inject()(jsonHandler: JsonHttpErrorHandler,
                             htmlHandler: DefaultHttpErrorHandler)
  extends PreferredMediaTypeHttpErrorHandler(
    "application/json" -> jsonHandler,
    "text/html" -> htmlHandler,
    "text/plain" -> plainTextHttpErrorHandler)