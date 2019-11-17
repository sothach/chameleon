package security

import java.time.{LocalDateTime, ZoneOffset}

import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.google.inject.Inject
import model.UserRole.UserRole
import model.{EmailAddress, User, UserRole}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Results._
import play.api.mvc._
import services.ChronoService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class JwtUtility(secretKey: String, dateTime: () => LocalDateTime) {
  val authHeader = "Authorization" // : Bearer
  val JwtSecretAlgo = "HS256"

  def createToken(payload: String): Option[String] = {
    val header = JwtHeader(JwtSecretAlgo)
    val claimsSet = JwtClaimsSet(payload)
    val token = JsonWebToken(header, claimsSet, secretKey)
    if(validate(token).nonEmpty) {
      Some(token)
    } else {
      None
    }
  }

  def createBearerToken(email: EmailAddress, role: UserRole, expires: LocalDateTime): Option[String] = {
    val expiry = expires.toEpochSecond(ZoneOffset.UTC)
    val payload = s"""{"email":"${email.address}","role":"$role","exp":$expiry}"""
    createToken(payload).map(token => s"Bearer $token")
  }

  def validate(jwtToken: String): Map[String, String] =
    decodePayload(jwtToken) match {
      case payload if payload.nonEmpty && checkClaims(payload) =>
        payload
      case _ =>
        Map.empty
    }

  def decodePayload(jwtToken: String): Map[String, String] =
    (jwtToken match {
      case JsonWebToken(_, claimsSet, _) =>
        claimsSet.asSimpleMap.toOption
      case _ => None
    }).getOrElse(Map.empty)

  private def checkClaims(claims: Map[String, String]) =
    (for {
      _ <- claims.get("email")
      _ <- claims.get("role").flatMap(r => Try(UserRole.withName(r)).toOption)
      exp <- claims.get("exp").filter(_.toLong > dateTime().toEpochSecond(ZoneOffset.UTC))
    } yield exp).isDefined

}

case class UserRequest[A](user: User, request: Request[A]) extends WrappedRequest(request)

class Authorization @Inject()(chronoService: ChronoService,
                              configuration: Configuration,
                              messagesApi: MessagesApi,
                              val parser: BodyParsers.Default)
                             (implicit val executionContext: ExecutionContext)
    extends ActionBuilder[UserRequest, AnyContent] {
  private val jwtUtility = new JwtUtility(configuration
    .getOptional[String]("play.http.secret.key").getOrElse(""),
    () => chronoService.now)

  override def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] = {
    val authToken = (request: Request[A]) =>
      request.headers.get(jwtUtility.authHeader) map {
        case token if token.startsWith("Bearer ") =>
          token.drop(7)
      }
    implicit val req: Request[A] = request
    val jwtToken = authToken(request)
    val payload = jwtUtility.validate(jwtToken.getOrElse(""))
    lazy val messages = messagesApi.preferred(request)
    lazy val unauthorized = Future.successful(
      Unauthorized(messages("request.invalid.credential")))
    buildUser(payload) match {
      case Some(userInfo) =>
        block(UserRequest(userInfo, request))
      case _ =>
        unauthorized
    }
  }

  private def buildUser(values: Map[String, String]): Option[User] = for {
    emailAddr <- values.get("email")
    email <- EmailAddress(emailAddr.trim)
    role <- values.get("role")
  } yield (User(email, UserRole.withName(role.trim)))
}
