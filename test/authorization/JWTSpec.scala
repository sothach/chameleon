package authorization

import java.time.LocalDateTime

import model.EmailAddress
import model.UserRole.Customer
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import security.JwtUtility

class JWTSpec extends FlatSpec with Matchers with OptionValues {
  private val secretKey = "<can-you-guess-it?>"
  private val testEmail = EmailAddress("test@mail.org").value

  "A valid JWT token" should "be verified" in {
    val dateTime = LocalDateTime.parse("2019-11-14T10:30:00")
    val subject = new JwtUtility(secretKey, () => dateTime)
    subject.createBearerToken(testEmail,Customer,dateTime.plusMinutes(5)) should not be(empty)
  }

  "An expired JWT token" should "be rejected" in {
    val dateTime = LocalDateTime.parse("2019-11-14T10:30:00")
    val subject = new JwtUtility(secretKey, () => dateTime)
    subject.createBearerToken(testEmail,Customer,dateTime.minusMinutes(5)) should be(empty)
  }

  "An invalid JWT payload" should "be rejected" in {
    val payload = s"""{"email":"test@mail.org","role":"Customer","exp":}"""
    new JwtUtility(secretKey, () => LocalDateTime.now).createToken(payload) should be(empty)
  }


}
