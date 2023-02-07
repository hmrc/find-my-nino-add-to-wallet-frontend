package base

import org.scalatestplus.play.ServerProvider
import play.api.Application
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.libs.ws.{DefaultWSCookie, WSClient, WSResponse}

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS}


trait CreateRequestHelper extends ServerProvider {

  val defaultSeconds = 5
  implicit val defaultDuration: FiniteDuration = Duration.apply(defaultSeconds, SECONDS)

  val app: Application

  lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])

  implicit val defaultCookie = DefaultWSCookie("CSRF-Token","nocheck")

  def bakeCookie(sessionKvs: (String, String)*): (String, String) =
    HeaderNames.COOKIE -> SessionCookieBaker.bakeSessionCookie(sessionKvs.toMap)

  def getRequest(path: String, follow: Boolean = false, headers: Seq[(String, String)] = Seq.empty)(sessionKvs: (String, String)*): Future[WSResponse] = {
    val allHeaders = headers ++ Seq("Csrf-Token" -> "nocheck", bakeCookie(sessionKvs:_*))
    ws.url(s"http://localhost:$port/find-my-nino-add-to-wallet-frontend$path")
      .withHttpHeaders(allHeaders: _*)
      .withFollowRedirects(follow)
      .get()
  }

  def postRequest(path: String, formJson: JsValue, follow: Boolean = false, headers: Seq[(String, String)] = Seq.empty)
                 (sessionKvs: (String, String)*)(): Future[WSResponse] = {

    val allHeaders = headers ++ Seq("Csrf-Token" -> "nocheck", bakeCookie(sessionKvs:_*))
    ws.url(s"http://localhost:$port/find-my-nino-add-to-wallet-frontend$path")
      .withHttpHeaders(allHeaders: _*)
      .withFollowRedirects(follow)
      .post(formJson)
  }
}
