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
import models.nps.CRNUpliftRequest
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{CONTENT_TYPE, JSON}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import util.WireMockHelper

import scala.concurrent.ExecutionContext

class NPSConnectorSpec
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

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  lazy val npsConnector: NPSConnector = app.injector.instanceOf[NPSConnector]

  val nino: String                  = "AA123456A"
  val requestBody: CRNUpliftRequest = CRNUpliftRequest("test", "test", "01/01/1990")
  val url: String                   = s"/find-my-nino-add-to-wallet/adult-registration/$nino"

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

  val jsonUnprocessableEntityAlreadyAdult: String =
    s"""
       |{
       |  "failures": [
       |    {
       |      "reason": "Already an adult account",
       |      "code": "63492"
       |    }
       |  ]
       |}
       |""".stripMargin

  def stubPut(url: String, responseStatus: Int, responseBody: Option[String] = None): StubMapping =
    server.stubFor {
      val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, JSON)
      val response     = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))
      put(url).willReturn(response)
    }

  "upliftCRN" must {

    "return Right(HttpResponse) when response is OK" in {
      stubPut(url, OK, Some(Json.obj("message" -> "success").toString()))

      val result = npsConnector.upliftCRN(nino, requestBody).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(HttpResponse(INTERNAL_SERVER_ERROR, "")).status mustBe OK
    }

    "return Right(HttpResponse) with UNPROCESSABLE_ENTITY when response body contains alreadyAnAdultErrorCode" in {
      stubPut(url, UNPROCESSABLE_ENTITY, Some(jsonUnprocessableEntityAlreadyAdult))

      val result = npsConnector.upliftCRN(nino, requestBody).value.futureValue

      result mustBe a[Right[_, _]]
      result.getOrElse(HttpResponse(INTERNAL_SERVER_ERROR, "")).status mustBe UNPROCESSABLE_ENTITY
    }

    "return Left(UpstreamErrorResponse) with UNPROCESSABLE_ENTITY when response body does NOT contain alreadyAnAdultErrorCode" in {
      stubPut(url, UNPROCESSABLE_ENTITY, Some(jsonUnprocessableEntity))

      val result = npsConnector.upliftCRN(nino, requestBody).value.futureValue

      result mustBe a[Left[_, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe UNPROCESSABLE_ENTITY
    }

    "return NOT_FOUND when called with an unknown nino" in {
      stubPut(url, NOT_FOUND, None)

      val result = npsConnector.upliftCRN(nino, requestBody).value.futureValue

      result mustBe a[Left[_, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe NOT_FOUND
    }

    "return given status code when an unexpected status is returned" in {
      stubPut(url, IM_A_TEAPOT, None)

      val result = npsConnector.upliftCRN(nino, requestBody).value.futureValue

      result mustBe a[Left[_, _]]
      result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe IM_A_TEAPOT
    }
  }
}
