/*
 * Copyright 2019 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.scalatest.matchers._
import play.api.http.HeaderNames
import play.api.libs.json.Reads
import play.api.libs.ws.WSResponse

trait CustomMatchers {
  def httpStatus(expectedValue: Int): HavePropertyMatcher[WSResponse, Int] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.status == expectedValue,
      "httpStatus",
      expectedValue,
      response.status
    )

  def redirectLocation(expectedValue: String): HavePropertyMatcher[WSResponse, Option[String]] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.header("Location").contains(expectedValue),
      "headerLocation",
      Some(expectedValue),
      response.header("Location")
    )

  def continueUrl(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.header(HeaderNames.LOCATION).getOrElse("") == expectedValue,
      "continueUrl",
      expectedValue,
      response.header(HeaderNames.LOCATION).getOrElse("")
    )

  def jsonBodyAs[T](expectedValue: T)(implicit reads: Reads[T]): HavePropertyMatcher[WSResponse, T] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.json.as[T] == expectedValue,
      "response.jsonBody",
      expectedValue,
      response.json.as[T]
    )

  def bodyAs(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.body == expectedValue,
      "response.body",
      expectedValue,
      response.body
    )

  def contentExists(content: String): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.body.contains(content),
      "response.body",
      content,
      response.body
    )

  def contentDoesNotExist(content: String): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => HavePropertyMatchResult(
      !response.body.contains(content),
      "response.body",
      content,
      response.body
    )


  def titleOf(expectedValue: String): HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => HavePropertyMatchResult(
      Jsoup.parse(response.body).title.contains(expectedValue),
      "response.title",
      expectedValue,
      Jsoup.parse(response.body).title
    )

  val emptyBody: HavePropertyMatcher[WSResponse, String] =
    (response: WSResponse) => HavePropertyMatchResult(
      response.body == "",
      "emptyBody",
      "",
      response.body
    )
}