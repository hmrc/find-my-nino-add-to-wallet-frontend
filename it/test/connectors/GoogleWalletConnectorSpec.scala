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

package connectors

import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results.InternalServerError
import play.api.test.{DefaultAwaitTimeout, Injecting}
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
    Map("microservice.services.find-my-nino-add-to-wallet-service.port" -> server.port())
  )

  val delay = 5000

  val passId: String = "passId"
  val fakeName: String = "fakeName"
  val fakeNino:String = "fakeNino"
  val googlePassUrl = " https://pay.google.com/gp/v/save/eyJhbGci6IkpXVCJ9"
  val createGooglePassDetails = GooglePassDetails(fakeName, fakeNino)
  val googlePassCardBytes: Array[Byte] = Array(99, 71, 86, 121, 99, 50, 57, 117, 82, 71, 86, 48, 89, 87, 108, 115, 99, 49, 78, 48, 99, 109, 108, 117, 90, 119, 61, 61)
  val googlePassUrlImage = Base64.getEncoder.encodeToString(googlePassCardBytes)

  val errMsg = Json.obj(
    "status" -> "500",
    "message" -> "someMsg"
  )


  trait SpecSetup {

    def url: String

    lazy val connector = {
      val httpClient = app.injector.instanceOf[HttpClientV2]
      val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
      new GoogleWalletConnector(frontendAppConfig, httpClient)
    }
  }

  "Calling get pass url by pass Id" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-google-pass-url?passId=$passId"
    }

    "return Google pass when called with an existing pass Id" in new LocalSetup {
      stubGet(url, OK, Some(googlePassUrl))
      val result = connector.getGooglePassUrl(passId).futureValue.get
      result mustBe googlePassUrl
    }

    "return empty when called with an unknown passId" in new LocalSetup {
      stubGet(url, OK, None)
      val result = connector.getGooglePassUrl(passId).futureValue.get
      result mustBe ""

    }

    "return None when NOT_FOUND status returned" in new LocalSetup {
      stubGet(url, NOT_FOUND, None)
      val result = connector.getGooglePassUrl(passId).futureValue
      result mustBe None
    }
    "throw an exception when unexpected status returned" in new LocalSetup {
      stubGet(url, IM_A_TEAPOT, None)
      assertThrows[RuntimeException] {
        val result = connector.getGooglePassUrl(passId).futureValue
      }
    }
  }

  "Calling get google qr code by pass Id" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-google-qr-code?passId=$passId"
    }

    "return Google pass when called with an existing pass Id" in new LocalSetup {
      stubGet(url, OK, Some(googlePassUrlImage))
      val result = connector.getGooglePassQrCode(passId).futureValue.get
      result mustBe googlePassCardBytes
    }

    "return empty Array when called with an unknown passId" in new LocalSetup {
      stubGet(url, OK, None)
      val result = connector.getGooglePassQrCode(passId).futureValue.get
      result mustBe Array[Byte]()
    }

    "return None when NOT_FOUND status returned" in new LocalSetup {
      stubGet(url, NOT_FOUND, None)
      val result = connector.getGooglePassQrCode(passId).futureValue
      result mustBe None
    }
    "throw an exception when unexpected status returned" in new LocalSetup {
      stubGet(url, IM_A_TEAPOT, None)
      assertThrows[RuntimeException] {
        val result = connector.getGooglePassQrCode(passId).futureValue
      }
    }
  }

  "Calling create google pass" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/create-google-pass-with-credentials"
    }

    "return OK when called create google pass" in new LocalSetup {
      stubPost(url, OK, Some(Json.toJson(createGooglePassDetails).toString()), Some(passId))
      val result: String = connector.createGooglePass(createGooglePassDetails.fullName, createGooglePassDetails.nino).futureValue.get
      result mustBe passId
    }

    "return error when called create google pass " in new LocalSetup {
      stubWithDelay(url, INTERNAL_SERVER_ERROR, Some(Json.toJson(createGooglePassDetails).toString()), None, delay)
      val result = connector.createGooglePass(createGooglePassDetails.fullName, createGooglePassDetails.nino)
        .value.getOrElse(InternalServerError(Json.toJson(errMsg)))
      result mustBe InternalServerError(Json.toJson(errMsg))
    }
    "throw an exception when expected status returned" in new LocalSetup {
      stubWithDelay(url, IM_A_TEAPOT, None, None, delay)
      assertThrows[RuntimeException] {
        val result = connector.createGooglePass(createGooglePassDetails.fullName, createGooglePassDetails.nino).futureValue
      }
    }
  }
}