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

package util

import com.github.tomakehurst.wiremock.client.WireMock.{unauthorized, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object Stubs {

  private val FMNRetrievals: String =
    """
      |{
      |	"authorise": [{
      |		"authProviders": ["GovernmentGateway"]
      |	}],
      |	"retrieve": ["nino", "affinityGroup", "allEnrolments", "optionalCredentials", "credentialStrength",
      | "confidenceLevel", "trustedHelper", "profile", "internalId", "credentialRole" ]
      |}
      |""".stripMargin

  def userLoggedInFMNUser(testUserJson: String): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(equalToJson(FMNRetrievals))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(testUserJson)
        )
    )

  def userLoggedInIsNotFMNUser(response: String): StubMapping =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(equalToJson(FMNRetrievals))
        .willReturn(
          unauthorized.withHeader("WWW-Authenticate", s"""MDTP detail="$response"""")
        )
    )
}
