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

package connectors

import config.FrontendAppConfig
import models.individualDetails.IndividualDetails
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import util.Fixtures.fakeIndividualDetails
import util.WireMockHelper

class IndividualDetailsConnectorSpec
    extends ConnectorSpec
    with WireMockHelper
    with MockitoSugar
    with DefaultAwaitTimeout
    with Injecting {

  override implicit lazy val app: Application = app(
    Map(
      "microservice.services.individual-details.port" -> server.port(),
      "microservice.services.individual-details.host" -> "127.0.0.1"
    )
  )

  trait Setup {
    val nino: String      = "AA123456A"
    val sessionId: String = "session-123"
    val url: String       =
      s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"

    val jsonBody: String = Json.toJson(fakeIndividualDetails).toString()

    lazy val connector: IndividualDetailsConnector = {
      val httpClientV2       = inject[HttpClientV2]
      val appConfig          = inject[FrontendAppConfig]
      val httpClientResponse = inject[HttpClientResponse]
      new IndividualDetailsConnector(httpClientV2, appConfig, httpClientResponse)
    }
  }

  "getIndividualDetails" should {

    "return individualDetails when API call succeeds" in new Setup {
      stubGet(url, OK, Some(jsonBody))

      val result: Either[UpstreamErrorResponse, IndividualDetails] =
        connector.getIndividualDetails(nino, sessionId).value.futureValue

      result mustBe a[Right[_, _]]
      result match {
        case Right(idd: IndividualDetails) =>
          idd.nino mustBe fakeIndividualDetails.nino
        case _                             => fail("Expected Right[individualDetails]")
      }
    }

    List(
      INTERNAL_SERVER_ERROR,
      BAD_REQUEST,
      NOT_FOUND,
      TOO_MANY_REQUESTS,
      REQUEST_TIMEOUT,
      SERVICE_UNAVAILABLE,
      BAD_GATEWAY
    ).foreach { errorStatus =>
      s"return UpstreamErrorResponse($errorStatus) when API call fails" in new Setup {
        stubGet(url, errorStatus, None)

        val result: Either[UpstreamErrorResponse, IndividualDetails] =
          connector.getIndividualDetails(nino, sessionId).value.futureValue

        result mustBe a[Left[UpstreamErrorResponse, _]]
        result.swap.toOption.get.statusCode mustBe errorStatus
      }
    }
  }

  trait SetupForDelete {
    val nino: String                               = "AA123456A"
    val sessionId: String                          = "session-123"
    val url: String                                =
      s"/find-my-nino-add-to-wallet/individuals/details/cache/NINO/${nino.take(8)}"
    lazy val connector: IndividualDetailsConnector = {
      val httpClientV2       = inject[HttpClientV2]
      val appConfig          = inject[FrontendAppConfig]
      val httpClientResponse = inject[HttpClientResponse]
      new IndividualDetailsConnector(httpClientV2, appConfig, httpClientResponse)
    }
  }

  "deleteIndividualDetails" should {
    "remove individualDetails when API call succeeds" in new SetupForDelete {
      stubDelete(url, OK, Some("true"))

      val result: Either[UpstreamErrorResponse, Boolean] = connector.deleteIndividualDetails(nino).value.futureValue

      result mustBe a[Right[_, _]]
      result match {
        case Right(d: Boolean) =>
          d mustBe true
        case _                 => fail("Expected true")
      }
    }
  }

}
