/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import base.IntegrationSpecBase
import base.WiremockHelper.wiremockPort
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys

class ApplicationControllerISpec extends IntegrationSpecBase {

  private val journeyId = "fake-journey-id"
  private val url = s"/save-your-national-insurance-number/identity-check-complete?journeyId=$journeyId"
  private val ivUrl = s"/mdtp/journey/journeyId/$journeyId"

  override def config: Map[String, _] = super.config ++ Map(
    "microservice.services.identity-verification-frontend.port" -> wiremockPort
  )


  "Accessing the uplift journey outcome" must {

    "render success view when IV result is Success" in {
      val jsonBody = """{"journeyResult": "Success"}""".stripMargin

      wireMockServer.stubFor(get(urlEqualTo(ivUrl)).willReturn(ok(jsonBody)))

      val request = FakeRequest(GET, url).withSession(SessionKeys.sessionId -> "FAKE_SESSION_ID")

      val result = route(app, request).get

      status(result) mustBe OK
      contentAsString(result) must include("We have confirmed your identity")
    }

    "render locked out view when IV result is LockedOut" in {
      val jsonBody = """{"journeyResult": "LockedOut"}""".stripMargin

      wireMockServer.stubFor(
        get(urlEqualTo(ivUrl)).willReturn(ok(jsonBody))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.sessionId -> "FAKE_SESSION_ID")
      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("You have tried to confirm your identity too many times")
    }

    "render cannot confirm identity view when IV result is InsufficientEvidence" in {
      val jsonBody = """{"journeyResult": "InsufficientEvidence"}""".stripMargin

      wireMockServer.stubFor(
        get(urlEqualTo(ivUrl)).willReturn(ok(jsonBody))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.sessionId -> "FAKE_SESSION_ID")
      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "render cannot confirm identity view when IV result is PreconditionFailed" in {
      val jsonBody = """{"journeyResult": "PreconditionFailed"}""".stripMargin

      wireMockServer.stubFor(
        get(urlEqualTo(ivUrl)).willReturn(ok(jsonBody))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.sessionId -> "FAKE_SESSION_ID")
      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED
      contentAsString(result) must include("We cannot confirm your identity")
    }

    "render technical issue view with 424 when IV result is TechnicalIssue" in {
      val jsonBody = """{"journeyResult": "TechnicalIssue"}""".stripMargin

      wireMockServer.stubFor(
        get(urlEqualTo(ivUrl)).willReturn(ok(jsonBody))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.sessionId -> "FAKE_SESSION_ID")
      val result = route(app, request).get

      status(result) mustBe FAILED_DEPENDENCY
      contentAsString(result) must include("Sorry, we are currently experiencing technical issues")
    }

    "render technical issue view with 424 when IV result is invalid response" in {
      val jsonBody = """{"journeyResult": "some random response"}""".stripMargin

      wireMockServer.stubFor(
        get(urlEqualTo(ivUrl)).willReturn(ok(jsonBody))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.sessionId -> "FAKE_SESSION_ID")
      val result = route(app, request).get

      status(result) mustBe FAILED_DEPENDENCY
      contentAsString(result) must include("Sorry, we are currently experiencing technical issues")
    }

    "render technical issue view with 424 when there is no journeyId or token in the request url" in {
      val urlWithoutJourneyId = "/save-your-national-insurance-number/identity-check-complete"

      val request = FakeRequest(GET, urlWithoutJourneyId).withSession(SessionKeys.sessionId -> "FAKE_SESSION_ID")
      val result = route(app, request).get

      status(result) mustBe FAILED_DEPENDENCY
      contentAsString(result) must include("Sorry, we are currently experiencing technical issues")
    }

    "render technical issue view with 500 when upstream IV call fails" in {
      wireMockServer.stubFor(
        get(urlEqualTo(ivUrl)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val request = FakeRequest(GET, url).withSession(SessionKeys.sessionId -> "FAKE_SESSION_ID")
      val result = route(app, request).get

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) must include("Sorry, we are currently experiencing technical issues")
    }
  }
}
