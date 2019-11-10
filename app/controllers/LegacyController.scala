package controllers

import javax.inject._
import model.JobSpecification
import play.api.Logger
import play.api.mvc._

@Singleton
class LegacyController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  private val logger = Logger(this.getClass)

  def redirect(request: JobSpecification): Action[AnyContent]= Action {
    Redirect(routes.LegacyController.request(request))
  }

  def request(request: JobSpecification): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    logger.debug(s"solve: $request")
    Ok("IMPOSSIBLE")
  }
}
