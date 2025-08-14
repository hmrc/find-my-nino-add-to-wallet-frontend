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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, get}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.FrontendAppConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{CONTENT_TYPE, JSON}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import util.Fixtures.*
import util.WireMockHelper

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

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

  lazy val connector: IndividualDetailsConnector = {
    val httpClientV2       = app.injector.instanceOf[HttpClientV2]
    val frontendAppConfig  = app.injector.instanceOf[FrontendAppConfig]
    val httpClientResponse = app.injector.instanceOf[HttpClientResponse]
    new IndividualDetailsConnector(httpClientV2, frontendAppConfig, httpClientResponse)
  }

  val nino: String                                     = "AA123456A"
  val sessionId: String                                = "test-session-id"
  val fakeIndividualDetailsJson: String                = Json.toJson(fakeIndividualDetails).toString()
  val url: String                                      = s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

  private def stubGet(url: String, responseStatus: Int, responseBody: Option[String] = None): StubMapping =
    server.stubFor {
      val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, JSON)
      val response     = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))
      get(url).willReturn(response)
    }

  private def stubDelete(url: String, responseStatus: Int, responseBody: Option[String]): StubMapping = server.stubFor {
    val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, JSON)
    val response     = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))

    delete(url).willReturn(response)
  }

  "IndividualDetailsConnector#getIndividualDetails" should {

    "should return individualDetails when API call succeeds" in {
      stubGet(url, OK, Some(fakeIndividualDetailsJson))

      val result = connector.getIndividualDetails(nino, "testSessionId").value

      whenReady(result) {
        case Right(cache) =>
          cache.nino mustBe "AB123456C"
        case Left(err)    =>
          fail(s"Expected Right but got Left: $err")
      }
    }

    "return Left(UpstreamErrorResponse) when API fails" in {
      stubGet(url, INTERNAL_SERVER_ERROR, None)

      val result = connector.getIndividualDetails(nino, sessionId).value.futureValue

      result mustBe a[Left[_, _]]
      result.swap.getOrElse(fail("Expected Left but got Right")).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "IndividualDetailsConnector#deleteIndividualDetails" must {
    "return unit for deleteIndividualDetails when successful" in {
      val url: String = s"/find-my-nino-add-to-wallet/individuals/details/cache/NINO/${nino.take(8)}"
      stubDelete(url, OK, Some("true"))
      val result      = Await.result(connector.deleteIndividualDetails(nino).value, Duration.Inf)
      result mustBe Right((): Unit)
    }
  }
}
