/*
 * Copyright 2023 HM Revenue & Customs
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

import config.ConfigDecorator
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results.InternalServerError
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.http.HttpClient
import util.Fixtures.buildPersonDetails
import util.WireMockHelper

import java.util.Base64

class ApplePassConnectorSpec extends ConnectorSpec
  with WireMockHelper
  with MockitoSugar
  with DefaultAwaitTimeout
  with Injecting {

  implicit val writes: Writes[ApplePassDetails] = Json.writes[ApplePassDetails]

  override implicit lazy val app: Application = app(
    Map("microservice.services.find-my-nino-add-to-wallet-service.port" -> server.port())
  )

  val delay = 5000
  val pd = buildPersonDetails
  val jsonPd = Json.toJson(pd)

  val passId: String = "passId"
  val personDetailsId: String = "pdId"
  val applePassCardBytes: Array[Byte] = Array(99, 71, 86, 121, 99, 50, 57, 117, 82, 71, 86, 48, 89, 87, 108, 115, 99, 49, 78, 48, 99, 109, 108, 117, 90, 119, 61, 61)
  val applePassCard = Base64.getEncoder.encodeToString(applePassCardBytes)
  val fakeName: String = "fakeName"
  val fakeNino:String = "fakeNino"
  val createApplePassDetails = ApplePassDetails(fakeName, fakeNino)

  val errMsg = Json.obj(
    "status" -> "500",
    "message" -> "someMsg"
  )


  trait SpecSetup {

    def url: String

    lazy val connector = {
      val httpClient = app.injector.instanceOf[HttpClient]
      val config = app.injector.instanceOf[ConfigDecorator]
      new ApplePassConnector(config,httpClient)
    }
  }

  "Calling get personDetails" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-person-details?pdId=$personDetailsId"
    }

    "return OK when called with an existing person details Id" in new LocalSetup {
      stubGet(url, OK, Some("personDetailsString"))
      val result = connector.getPersonDetails(personDetailsId).futureValue.get
      result mustBe "personDetailsString"
    }

    "return NOT_FOUND when called with an unknown personDetailsId" in new LocalSetup {
      stubGet(url, OK, None)
      val result = connector.getPersonDetails(personDetailsId).futureValue.get
      result mustBe ""

    }
  }

  "Calling get pass card by pass Id" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-pass-card?passId=$passId"
    }

    "return Pass when called with an existing pass Id" in new LocalSetup {
      stubGet(url, OK, Some(applePassCard))
      val result = connector.getApplePass(passId).futureValue.get
      result mustBe applePassCardBytes
    }

    "return empty Array when called with an unknown passId" in new LocalSetup {
      stubGet(url, OK, None)
      val result = connector.getApplePass(passId).futureValue.get
      result mustBe Array()

    }
  }

  "Calling getPass card by name and nino" must {

    trait LocalSetup extends SpecSetup {
      def url: String =
        s"/find-my-nino-add-to-wallet/get-pass-details-by-name-and-nino?fullName=$fakeName&nino=$fakeNino"
    }

    "return Pass when called with an existing name and nino" in new LocalSetup {
      stubGet(url, OK, Some(applePassCard))
      val result = connector.getApplePassByNameAndNino(fakeName, fakeNino).futureValue.get
      result mustBe applePassCardBytes
    }

    "return empty Array when called with an unknown passId" in new LocalSetup {
      stubGet(url, OK, None)
      val result = connector.getApplePassByNameAndNino(fakeName, fakeNino).futureValue.get
      result mustBe Array()
    }
  }

  "Calling get qr code by pass Id" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/get-qr-code?passId=$passId"
    }

    "return Pass when called with an existing pass Id" in new LocalSetup {
      stubGet(url, OK, Some(applePassCard))
      val result = connector.getQrCode(passId).futureValue.get
      result mustBe applePassCardBytes
    }

    "return empty Array when called with an unknown passId" in new LocalSetup {
      stubGet(url, OK, None)
      val result = connector.getQrCode(passId).futureValue.get
      result mustBe Array()

    }
  }

  "Calling create personDetails" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/create-person-details"
    }

    "return OK when called create person details" in new LocalSetup {
      stubPost(url, OK, Some(jsonPd.toString()), Some(personDetailsId))
      val result = connector.createPersonDetailsRow(pd).futureValue.get
      result mustBe personDetailsId
    }

    "return error when called create person details" in new LocalSetup {
      stubWithDelay(url, INTERNAL_SERVER_ERROR, Some(jsonPd.toString()), None, delay)
      val result = connector.createPersonDetailsRow(pd).value.getOrElse(InternalServerError(Json.toJson(errMsg)))
      result mustBe InternalServerError(Json.toJson(errMsg))
    }

  }

  "Calling create pass" must {

    trait LocalSetup extends SpecSetup {
      def url: String = s"/find-my-nino-add-to-wallet/create-apple-pass"
    }

    "return OK when called create pass" in new LocalSetup {
      stubPost(url, OK, Some(Json.toJson(createApplePassDetails).toString()), Some(personDetailsId))
      val result = connector.createApplePass(createApplePassDetails.fullName,createApplePassDetails.nino).futureValue.get
      result mustBe personDetailsId
    }

    "return error when called create pass " in new LocalSetup {
      stubWithDelay(url, INTERNAL_SERVER_ERROR, Some(Json.toJson(createApplePassDetails).toString()), None, delay)
      val result = connector.createApplePass(createApplePassDetails.fullName,createApplePassDetails.nino)
        .value.getOrElse(InternalServerError(Json.toJson(errMsg)))
      result mustBe InternalServerError(Json.toJson(errMsg))
    }
}}
