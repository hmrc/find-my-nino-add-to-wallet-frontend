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
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import util.Fixtures.fakeIndividualDetails
import util.WireMockHelper

class IndividualDetailsConnectorSpec
  extends ConnectorSpec with WireMockHelper with MockitoSugar with DefaultAwaitTimeout with Injecting {

  override implicit lazy val app: Application = app(
    Map(
      "microservice.services.individual-details.port" -> server.port(),
      "microservice.services.individual-details.host" -> "127.0.0.1"
    )
  )

  trait SpecSetup {
    val nino: String = "AA123456A"
    val resolveMerge: String = "Y"
    val url: String = s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/$resolveMerge"
    val responseBody: String = Json.toJson(fakeIndividualDetails).toString()

    lazy val connector: IndividualDetailsConnector = {
      val httpClientV2 = app.injector.instanceOf[HttpClientV2]
      val appConfig = app.injector.instanceOf[FrontendAppConfig]
      val httpClientResponse = app.injector.instanceOf[HttpClientResponse]
      new IndividualDetailsConnector(httpClientV2, appConfig, httpClientResponse)
    }
  }


  "Calling getIndividualDetails" must {
    "return HttpResponse with 200 OK" in new SpecSetup {
      stubGet(url, OK, Some(responseBody))

      val result = connector.getIndividualDetails(nino, resolveMerge).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(HttpResponse(IM_A_TEAPOT, "")).status mustBe OK
    }

    List(
      INTERNAL_SERVER_ERROR,
      BAD_REQUEST,
      NOT_FOUND,
      TOO_MANY_REQUESTS,
      REQUEST_TIMEOUT,
      SERVICE_UNAVAILABLE,
      BAD_GATEWAY
    ).foreach { errorResponse =>
      s"return an UpstreamErrorResponse containing $errorResponse when API call fails" in new SpecSetup {
        stubGet(url, errorResponse, None)

        val result = connector.getIndividualDetails(nino, resolveMerge).value.futureValue

        result mustBe a[Left[UpstreamErrorResponse, _]]
        result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe errorResponse
      }
    }
  }
}