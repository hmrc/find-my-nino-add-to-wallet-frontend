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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.FrontendAppConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers.{CONTENT_TYPE, JSON}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.WireMockHelper

import java.util.Base64
import scala.concurrent.ExecutionContext

class AppleWalletConnectorSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with MockitoSugar
    with ScalaFutures {
  override def fakeApplication(): Application = {
    server.start()
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.find-my-nino-add-to-wallet-service.port" -> server.port(),
        "microservice.services.find-my-nino-add-to-wallet-service.host" -> "127.0.0.1"
      )
      .build()
  }

  implicit val appleWrites: Writes[ApplePassDetails] = Json.writes[ApplePassDetails]
  implicit val hc: HeaderCarrier                     = HeaderCarrier()
  implicit val ec: ExecutionContext                  = app.injector.instanceOf[ExecutionContext]

  val passId: String                     = "passId"
  val applePassCardBytes: Array[Byte]    = Array(99, 71, 86, 121, 99, 50, 57, 117, 82, 71, 86, 48, 89, 87, 108, 115, 99,
    49, 78, 48, 99, 109, 108, 117, 90, 119, 61, 61)
  val applePassCard: String              = Base64.getEncoder.encodeToString(applePassCardBytes)
  val applePassDetails: ApplePassDetails = ApplePassDetails("fakeName", "AA123456A")

  lazy val connector: AppleWalletConnector = {
    val httpClientV2       = app.injector.instanceOf[HttpClientV2]
    val frontendAppConfig  = app.injector.instanceOf[FrontendAppConfig]
    val httpClientResponse = app.injector.instanceOf[HttpClientResponse]
    new AppleWalletConnector(frontendAppConfig, httpClientV2, httpClientResponse)
  }

  def stubGet(url: String, responseStatus: Int, responseBody: Option[String]): StubMapping = server.stubFor {
    val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, JSON)
    val response     = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))

    get(url).willReturn(response)
  }

  def stubPost(
    url: String,
    responseStatus: Int,
    requestBody: Option[String],
    responseBody: Option[String]
  ): StubMapping = server.stubFor {
    val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, JSON)
    val response     = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))

    requestBody.fold(post(url).willReturn(response))(requestBody =>
      post(url).withRequestBody(equalToJson(requestBody)).willReturn(response)
    )
  }

  "getApplePass" must {
    val url: String = s"/find-my-nino-add-to-wallet/get-pass-card?passId=$passId"

    "return Right(Some(pass)) when called with an existing pass Id" in {
      stubGet(url, OK, Some(applePassCard))
      val result = connector.getApplePass(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None).get must contain theSameElementsAs applePassCardBytes
    }

    "return Right(None) when called with an unknown passId" in {
      stubGet(url, NOT_FOUND, None)
      val result = connector.getApplePass(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result mustBe Right(None)
    }

    "return Left(UpstreamErrorResponse) when API call fails" in {
      stubGet(url, INTERNAL_SERVER_ERROR, None)
      val result = connector.getApplePass(passId).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "getAppleQrCode" must {
    val url: String = s"/find-my-nino-add-to-wallet/get-qr-code?passId=$passId"

    "return Right(Some(qrCode)) when called with an existing pass Id" in {
      stubGet(url, OK, Some(applePassCard))
      val result = connector.getAppleQrCode(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None).get must contain theSameElementsAs applePassCardBytes
    }

    "return Right(None) when called with an unknown passId" in {
      stubGet(url, NOT_FOUND, None)
      val result = connector.getAppleQrCode(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result mustBe Right(None)
    }

    "return Left(UpstreamErrorResponse) when API call fails" in {
      stubGet(url, INTERNAL_SERVER_ERROR, None)
      val result = connector.getAppleQrCode(passId).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "createApplePass" must {
    val url: String = s"/find-my-nino-add-to-wallet/create-apple-pass"

    "return Some(passId) when valid data is passed" in {
      stubPost(url, OK, Some(Json.toJson(applePassDetails).toString()), Some(passId))
      val result = connector.createApplePass(applePassDetails.fullName, applePassDetails.nino).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None) mustBe Some(passId)
    }

    "return Left(UpstreamErrorResponse) when API call fails" in {
      stubPost(url, INTERNAL_SERVER_ERROR, None, None)
      val result = connector.createApplePass(applePassDetails.fullName, applePassDetails.nino).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }
}
