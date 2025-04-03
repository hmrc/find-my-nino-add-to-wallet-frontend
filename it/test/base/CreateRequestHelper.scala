/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package base

import org.scalatestplus.play.ServerProvider
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.libs.ws.{DefaultWSCookie, WSClient, WSResponse}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS}


trait CreateRequestHelper extends ServerProvider {

  val defaultSeconds = 5
  implicit val defaultDuration: FiniteDuration = Duration.apply(defaultSeconds, SECONDS)

  lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])

  implicit val defaultCookie: DefaultWSCookie = DefaultWSCookie("CSRF-Token", "nocheck")

  def bakeCookie(sessionKvs: (String, String)*): (String, String) =
    HeaderNames.COOKIE -> SessionCookieBaker.bakeSessionCookie(sessionKvs.toMap)

  def getRequest(path: String, follow: Boolean = false, headers: Seq[(String, String)] = Seq.empty)(sessionKvs: (String, String)*): Future[WSResponse] = {
    val allHeaders = headers ++ Seq("Csrf-Token" -> "nocheck", bakeCookie(sessionKvs:_*))
    ws.url(s"http://localhost:$port/save-your-national-insurance-number$path")
      .withHttpHeaders(allHeaders: _*)
      .withFollowRedirects(follow)
      .get()
  }

  def postRequest(path: String, formJson: JsValue, follow: Boolean = false, headers: Seq[(String, String)] = Seq.empty)
                 (sessionKvs: (String, String)*)(): Future[WSResponse] = {

    val allHeaders = headers ++ Seq("Csrf-Token" -> "nocheck", bakeCookie(sessionKvs:_*))
    ws.url(s"http://localhost:$port/save-your-national-insurance-number$path")
      .withHttpHeaders(allHeaders: _*)
      .withFollowRedirects(follow)
      .post(formJson)
  }
}
