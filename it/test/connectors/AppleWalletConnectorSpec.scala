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
import play.api.libs.json.{Json, Writes}
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import util.WireMockHelper

import java.util.Base64

class AppleWalletConnectorSpec extends ConnectorSpec
  with WireMockHelper
  with MockitoSugar
  with DefaultAwaitTimeout
  with Injecting {

  implicit val appleWrites: Writes[ApplePassDetails] = Json.writes[ApplePassDetails]

  override implicit lazy val app: Application = app(
    Map("microservice.services.find-my-nino-add-to-wallet-service.port" -> server.port(),
        "microservice.services.find-my-nino-add-to-wallet-service.host" -> "127.0.0.1")
  )

  val delay = 5000

  val passId: String = "passId"
  val applePassCardBytes: Array[Byte] = Array(99, 71, 86, 121, 99, 50, 57, 117, 82, 71, 86, 48, 89, 87, 108, 115, 99, 49, 78, 48, 99, 109, 108, 117, 90, 119, 61, 61)
  val applePassCard: String = Base64.getEncoder.encodeToString(applePassCardBytes)
  val fakeName: String = "fakeName"
  val fakeNino:String = "fakeNino"
  val createApplePassDetails: ApplePassDetails = ApplePassDetails(fakeName, fakeNino)

  trait SpecSetup {

    def url: String

    lazy val connector: AppleWalletConnector = {
      val httpClientV2 = app.injector.instanceOf[HttpClientV2]
      val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
      val httpClientResponse = app.injector.instanceOf[HttpClientResponse]
      new AppleWalletConnector(frontendAppConfig, httpClientV2, httpClientResponse)
    }
  }

  "Calling get pass card by pass Id" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-pass-card?passId=$passId"
    }

    "return Pass when called with an existing pass Id" in new LocalSetup {
      stubGet(url, OK, Some(applePassCard))
      val result = connector.getApplePass(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None).get must contain theSameElementsAs applePassCardBytes
    }

    "return None when called with an unknown passId" in new LocalSetup {
      stubGet(url, NOT_FOUND, None)
      val result = connector.getApplePass(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result mustBe Right(None)
    }

    "return Left(UpstreamErrorResponse) when API call fails" in new LocalSetup {
      stubGet(url, INTERNAL_SERVER_ERROR, None)
      val result = connector.getApplePass(passId).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "Calling get qr code by pass Id" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-qr-code?passId=$passId"
    }

    "return QR code when called with an existing pass Id" in new LocalSetup {
      stubGet(url, OK, Some(applePassCard))
      val result = connector.getAppleQrCode(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None).get must contain theSameElementsAs applePassCardBytes
    }

    "return None when called with an unknown passId" in new LocalSetup {
      stubGet(url, NOT_FOUND, None)
      val result = connector.getAppleQrCode(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result mustBe Right(None)
    }

    "return Left(UpstreamErrorResponse) when API call fails" in new LocalSetup {
      stubGet(url, INTERNAL_SERVER_ERROR, None)
      val result = connector.getAppleQrCode(passId).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "Calling create pass" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/create-apple-pass"
    }

    "return Some(passId) when called create pass" in new LocalSetup {
      stubPost(url, OK, Some(Json.toJson(createApplePassDetails).toString()), Some(passId))
      val result = connector.createApplePass(createApplePassDetails.fullName, createApplePassDetails.nino).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None) mustBe Some(passId)
    }

    "return Left(UpstreamErrorResponse) when API call fails" in new LocalSetup {
      stubWithDelay(url, INTERNAL_SERVER_ERROR, Some(Json.toJson(createApplePassDetails).toString()), None, delay)
      val result = connector.createApplePass(createApplePassDetails.fullName, createApplePassDetails.nino).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }
}