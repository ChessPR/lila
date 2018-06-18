package lila.common

import java.util.regex.Matcher.quoteReplacement

import ornicar.scalalib.Random
import play.api.mvc.{ Cookie, DiscardingCookie, Session, RequestHeader }
import old.play.Env.cookieBaker

object LilaCookie {

  private val domainRegex = """^.+(\.[^\.]+\.[^\.]+)$""".r

  private def domain(req: RequestHeader): String =
    domainRegex.replaceAllIn(req.domain, m => quoteReplacement(m group 1))

  val sessionId = "sid"

  def makeSessionId(implicit req: RequestHeader) = session(sessionId, Random secureString 10)

  def session(name: String, value: String)(implicit req: RequestHeader): Cookie = withSession { s =>
    s + (name -> value)
  }

  def newSession(implicit req: RequestHeader): Cookie = withSession(identity)

  def withSession(op: Session => Session)(implicit req: RequestHeader): Cookie = cookie(
    cookieBaker.COOKIE_NAME,
    Session.encode(cookieBaker.serialize(op(req.session)))
  )

  def cookie(name: String, value: String, maxAge: Option[Int] = None, httpOnly: Option[Boolean] = None)(implicit req: RequestHeader): Cookie = Cookie(
    name,
    value,
    maxAge orElse cookieBaker.maxAge orElse 86400.some,
    "/",
    domain(req).some,
    cookieBaker.secure || req.headers.get("X-Forwarded-Proto").contains("https"),
    httpOnly | cookieBaker.httpOnly
  )

  def discard(name: String)(implicit req: RequestHeader) =
    DiscardingCookie(name, "/", domain(req).some, Session.httpOnly)
}
