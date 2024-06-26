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
import models.nps.CRNUpliftRequest
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import util.WireMockHelper

class NPSConnectorSpec
  extends ConnectorSpec
    with WireMockHelper
    with MockitoSugar
    with DefaultAwaitTimeout
    with Injecting {

  override implicit lazy val app: Application = app(
    Map("microservice.services.find-my-nino-add-to-wallet-service.port" -> server.port(),
    )
  )

  val nino = "nino"

  val jsonUnprocessableEntity: String =
    s"""
       |{
       |  "failures": [
       |    {
       |      "reason": "Date of birth does not match",
       |      "code": "63484"
       |    }
       |  ]
       |}
       |""".stripMargin

  val jsonForbidden: String =
    s"""
       |{
       |  "reason": "Forbidden",
       |  "code": "403.2"
       |}
       |""".stripMargin

  val jsonBadRequest: String =
    s"""
       |{
       |  "failures": [
       |    {
       |      "reason": "HTTP message not readable",
       |      "code": "400.2"
       |    },
       |    {
       |      "reason": "Constraint Violation - Invalid/Missing input parameter",
       |      "code": "400.1"
       |    }
       |  ]
       |}
       |""".stripMargin


  trait SpecSetup {

    def url(nino: String): String

    lazy val connector: NPSConnector = {
      val httpClient2 = app.injector.instanceOf[HttpClientV2]
      val config = app.injector.instanceOf[FrontendAppConfig]
      new NPSConnector(httpClient2, config)
    }

    val body = mock[CRNUpliftRequest]
  }

  "NPS FMN Connector" must {

    trait LocalSetup extends SpecSetup {
      def url(nino: String): String = s"/find-my-nino-add-to-wallet/adult-registration/$nino"
    }

    "return 204 NO_CONTENT when called with a CRN" in new LocalSetup {
      stubPut(url(nino), NO_CONTENT, Some(Json.toJson(body).toString()), Some(""))
      val result: HttpResponse = connector.upliftCRN(nino, body).futureValue.leftSideValue

      result.status mustBe NO_CONTENT
      result.body mustBe ""
    }

    "return 400 BAD_REQUEST when called with invalid request object" in new LocalSetup {
      stubPut(url(nino), BAD_REQUEST, Some(Json.toJson(body).toString()), Some(jsonBadRequest))
      val result = connector.upliftCRN(nino, body).futureValue.leftSideValue

      result.status mustBe BAD_REQUEST
      result.body mustBe jsonBadRequest
    }

    "return 403 FORBIDDEN when called with forbidden request" in new LocalSetup {
      stubPut(url(nino), FORBIDDEN, Some(Json.toJson(body).toString()), Some(jsonForbidden))
      val result = connector.upliftCRN(nino, body).futureValue.leftSideValue

      result.status mustBe FORBIDDEN
      result.body mustBe jsonForbidden
    }

    "return 422 UNPROCESSABLE_ENTITY when the action cannot be completed" in new LocalSetup {
      stubPut(url(nino), UNPROCESSABLE_ENTITY, Some(Json.toJson(body).toString()), Some(jsonUnprocessableEntity))
      val result = connector.upliftCRN(nino, body).futureValue.leftSideValue

      result.status mustBe UNPROCESSABLE_ENTITY
      result.body mustBe jsonUnprocessableEntity
    }

    "return 404 NOT_FOUND when resource cannot be found" in new LocalSetup {
      stubPut(url(nino), NOT_FOUND, Some(Json.toJson(body).toString()), None)
      val result = connector.upliftCRN(nino, body).futureValue.leftSideValue

      result.status mustBe NOT_FOUND
      result.body mustBe ""
    }

    "return 500 INTERNAL_SERVER_ERROR when exception is thrown" in new LocalSetup {
      stubPut(url(nino), INTERNAL_SERVER_ERROR, Some(Json.toJson(body).toString()), None)

      val result = connector.upliftCRN(nino, body).futureValue.leftSideValue
      result.status mustBe INTERNAL_SERVER_ERROR
      result.body mustBe ""
    }

  }

}