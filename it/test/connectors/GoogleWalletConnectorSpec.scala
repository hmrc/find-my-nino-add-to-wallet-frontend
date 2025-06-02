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
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import util.WireMockHelper

import java.util.Base64

class GoogleWalletConnectorSpec extends ConnectorSpec
  with WireMockHelper
  with MockitoSugar
  with DefaultAwaitTimeout
  with Injecting {

  implicit val googleWrites: Writes[GooglePassDetails] = Json.writes[GooglePassDetails]

  override implicit lazy val app: Application = app(
    Map("microservice.services.find-my-nino-add-to-wallet-service.port" -> server.port(),
      "microservice.services.find-my-nino-add-to-wallet-service.host" -> "127.0.0.1"
    )
  )

  val delay = 5000

  val passId: String = "passId"
  val fakeName: String = "fakeName"
  val fakeNino:String = "fakeNino"
  val googlePassUrl = " https://pay.google.com/gp/v/save/eyJhbGci6IkpXVCJ9"
  val createGooglePassDetails: GooglePassDetails = GooglePassDetails(fakeName, fakeNino)
  val googlePassCardBytes: Array[Byte] = Array(99, 71, 86, 121, 99, 50, 57, 117, 82, 71, 86, 48, 89, 87, 108, 115, 99, 49, 78, 48, 99, 109, 108, 117, 90, 119, 61, 61)
  val googlePassUrlImage: String = Base64.getEncoder.encodeToString(googlePassCardBytes)


  trait SpecSetup {

    def url: String

    lazy val connector: GoogleWalletConnector = {
      val httpClient = app.injector.instanceOf[HttpClientV2]
      val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
      val httpClientResponse = app.injector.instanceOf[HttpClientResponse]
      new GoogleWalletConnector(frontendAppConfig, httpClient, httpClientResponse)
    }
  }

  "Calling get pass url by pass Id" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-google-pass-url?passId=$passId"
    }

    "return Google pass when called with an existing pass Id" in new LocalSetup {
      stubGet(url, OK, Some(googlePassUrl))
      val result: Either[UpstreamErrorResponse, Option[String]] = connector.getGooglePassUrl(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None).get mustBe googlePassUrl
    }

    "return None when called with an unknown passId" in new LocalSetup {
      stubGet(url, NOT_FOUND, None)
      val unknownPassId = "somePassId"
      val result: Either[UpstreamErrorResponse, Option[String]] = connector.getGooglePassUrl(unknownPassId).value.futureValue

      result mustBe a[Right[_, _]]
      result mustBe Right(None)
    }

    "return Left(UpstreamErrorResponse) when API call returns an unexpected status" in new LocalSetup {
      stubGet(url, NO_CONTENT, None)
      val result: Either[UpstreamErrorResponse, Option[String]] = connector.getGooglePassUrl(passId).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe NO_CONTENT
    }

    "return Left(UpstreamErrorResponse) when API call fails" in new LocalSetup {
      stubGet(url, INTERNAL_SERVER_ERROR, None)
      val result: Either[UpstreamErrorResponse, Option[String]] = connector.getGooglePassUrl(passId).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "Calling get google qr code by pass Id" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-google-qr-code?passId=$passId"
    }

    "return Google pass when called with an existing pass Id" in new LocalSetup {
      stubGet(url, OK, Some(googlePassUrlImage))
      val result: Either[UpstreamErrorResponse, Option[Array[Byte]]] = connector.getGooglePassQrCode(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None).get must contain theSameElementsAs googlePassCardBytes
    }

    "return None when called with an unknown passId" in new LocalSetup {
      stubGet(url, NOT_FOUND, None)
      val result: Either[UpstreamErrorResponse, Option[Array[Byte]]] = connector.getGooglePassQrCode(passId).value.futureValue

      result mustBe a[Right[_, _]]
      result mustBe Right(None)
    }

    "return Left(UpstreamErrorResponse) when API call returns an unexpected status" in new LocalSetup {
      stubGet(url, NO_CONTENT, None)
      val result: Either[UpstreamErrorResponse, Option[Array[Byte]]] = connector.getGooglePassQrCode(passId).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe NO_CONTENT
    }

    "return Left(UpstreamErrorResponse) when API call fails" in new LocalSetup {
      stubGet(url, INTERNAL_SERVER_ERROR, None)
      val result: Either[UpstreamErrorResponse, Option[Array[Byte]]] = connector.getGooglePassQrCode(passId).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "Calling create google pass" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/create-google-pass-with-credentials"
    }

    "return passId when called create google pass" in new LocalSetup {
      stubPost(url, OK, Some(Json.toJson(createGooglePassDetails).toString()), Some(passId))
      val result: Either[UpstreamErrorResponse, Some[String]] = connector.createGooglePass(createGooglePassDetails.fullName, createGooglePassDetails.nino).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(None).get mustBe passId
    }

    "return Left(UpstreamErrorResponse) when API call returns an unexpected status" in new LocalSetup {
      stubWithDelay(url, NO_CONTENT, Some(Json.toJson(createGooglePassDetails).toString()), None, delay)

      val result: Either[UpstreamErrorResponse, Some[String]] = connector.createGooglePass(createGooglePassDetails.fullName, createGooglePassDetails.nino).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe NO_CONTENT
    }

    "return Left(UpstreamErrorResponse) when API call fails" in new LocalSetup {
      stubWithDelay(url, INTERNAL_SERVER_ERROR, Some(Json.toJson(createGooglePassDetails).toString()), None, delay)

      val result: Either[UpstreamErrorResponse, Some[String]] = connector.createGooglePass(createGooglePassDetails.fullName, createGooglePassDetails.nino).value.futureValue

      result mustBe a[Left[UpstreamErrorResponse, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", IM_A_TEAPOT)).statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }
}