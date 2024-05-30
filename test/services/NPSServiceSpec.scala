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

package services

import base.SpecBase
import connectors.NPSConnector
import models.nps.CRNUpliftRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import uk.gov.hmrc.http._

import scala.concurrent.Future

class NPSServiceSpec extends SpecBase{

  val nino = "AA000003B"
  val npsRequest: CRNUpliftRequest = CRNUpliftRequest("test", "test", "01/01/1990")

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

  val jsonForbidden: String =
    s"""
       |{
       |  "reason": "Forbidden",
       |  "code": "403.2"
       |}
       |""".stripMargin

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


  private val mockNPSConnector = mock[NPSConnector]

  override implicit lazy val app: Application = applicationBuilder()
    .overrides(
      bind[NPSConnector].toInstance(mockNPSConnector)
    )

    .build()

  val npsService: NPSService = app.injector.instanceOf[NPSService]

  override def beforeEach(): Unit =
    reset(mockNPSConnector)

  "upliftCRN" - {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "when back end connector returns 204 (NO_CONTENT)" - {
      "return a right of NO_CONTENT" in {

        when(mockNPSConnector
          .upliftCRN(any, any)(any, any()))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

        val result = npsService.upliftCRN(nino, npsRequest)
        result.futureValue mustBe Right(NO_CONTENT)
      }
    }

    "when back end connector returns 422 (UNPROCESSABLE_ENTITY) with code 63492 (Already an adult account)" - {
      "return a right of NO_CONTENT" in {

        when(mockNPSConnector
          .upliftCRN(any, any)(any, any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, jsonUnprocessableEntityAlreadyAdult)))

        val result = npsService.upliftCRN(nino, npsRequest)
        result.futureValue mustBe Right(NO_CONTENT)
      }
    }

    "when back end connector returns 400 (BAD_REQUEST)" - {
      "return a bad request exception (BAD_REQUEST)" in {

        val response: HttpResponse = HttpResponse(BAD_REQUEST, jsonBadRequest)

        when(mockNPSConnector
          .upliftCRN(any, any)(any, any()))
          .thenReturn(Future.successful(response))

        val result = npsService.upliftCRN(nino, npsRequest)
        result.futureValue mustBe Left(BAD_REQUEST)
        }
      }
    }

    "when back end connector returns 403 (FORBIDDEN)" - {
      "return a bad request exception (FORBIDDEN)" in {

        val response: HttpResponse = HttpResponse(FORBIDDEN, jsonForbidden)

        when(mockNPSConnector
          .upliftCRN(any, any)(any, any()))
          .thenReturn(Future.successful(response))

        val result = npsService.upliftCRN(nino, npsRequest)
        result.futureValue mustBe Left(FORBIDDEN)
      }
    }

    "when back end connector returns 422 (UNPROCESSABLE_ENTITY)" - {
      "return a bad request exception (UNPROCESSABLE_ENTITY)" in {

        val response: HttpResponse = HttpResponse(UNPROCESSABLE_ENTITY, jsonUnprocessableEntity)

        when(mockNPSConnector
          .upliftCRN(any, any)(any, any()))
          .thenReturn(Future.successful(response))

        val result = npsService.upliftCRN(nino, npsRequest)
        result.futureValue mustBe Left(UNPROCESSABLE_ENTITY)
      }
    }

    "when back end connector returns 404 (NOT_FOUND)" - {
      "return a bad request exception (NOT_FOUND)" in {

        val response: HttpResponse = HttpResponse(NOT_FOUND, "")

        when(mockNPSConnector
          .upliftCRN(any, any)(any, any()))
          .thenReturn(Future.successful(response))

        val result = npsService.upliftCRN(nino, npsRequest)
        result.futureValue mustBe Left(NOT_FOUND)
      }
    }

    "when back end connector returns 500 (INTERNAL_SERVER_ERROR)" - {
      "return a bad request exception (INTERNAL_SERVER_ERROR)" in {

        val response: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, "Something went wrong")

        when(mockNPSConnector
          .upliftCRN(any, any)(any, any()))
          .thenReturn(Future.successful(response))

        val result = npsService.upliftCRN(nino, npsRequest)
        result.futureValue mustBe Left(INTERNAL_SERVER_ERROR)
    }
  }
}