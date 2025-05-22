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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.FrontendAppConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{CONTENT_TYPE, JSON}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.Fixtures.fakeIndividualDetails
import util.WireMockHelper

import scala.concurrent.ExecutionContext

class IndividualDetailsConnectorSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with MockitoSugar
    with ScalaFutures {

  override def fakeApplication(): Application = {
    server.start()
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.individual-details.port" -> server.port(),
        "microservice.services.individual-details.host" -> "127.0.0.1"
      )
      .build()
  }

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  lazy val connector: DefaultIndividualDetailsConnector = {
    val httpClientV2       = app.injector.instanceOf[HttpClientV2]
    val frontendAppConfig  = app.injector.instanceOf[FrontendAppConfig]
    val httpClientResponse = app.injector.instanceOf[HttpClientResponse]
    new DefaultIndividualDetailsConnector(httpClientV2, frontendAppConfig, httpClientResponse)
  }

  val nino: String                      = "AA123456A"
  val resolveMerge: String              = "Y"
  val url: String                       = s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/$resolveMerge"
  val fakeIndividualDetailsJson: String = Json.toJson(fakeIndividualDetails).toString()

  def stubGet(url: String, responseStatus: Int, responseBody: Option[String] = None): StubMapping =
    server.stubFor {
      val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, JSON)
      val response     = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))
      get(url).willReturn(response)
    }

  "DefaultIndividualDetailsConnector#getIndividualDetails" must {
    "return Right(IndividualDetails) when API returns 200 and valid JSON" in {
      stubGet(url, OK, Some(fakeIndividualDetailsJson))

      val result = connector.getIndividualDetails(nino, resolveMerge).value.futureValue

      result mustBe Right(fakeIndividualDetails)
    }

    "return Left(UpstreamErrorResponse) when API returns an error" in {
      stubGet(url, INTERNAL_SERVER_ERROR)

      val result = connector.getIndividualDetails(nino, resolveMerge).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(fail("Expected a Left but got a Right")).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }
}
