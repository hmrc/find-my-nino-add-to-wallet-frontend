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

package controllers

import base.SpecBase
import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{IndividualDetailsService, NPSService}
import uk.gov.hmrc.http.UpstreamErrorResponse
import util.IndividualDetailsFixtures
import util.Fixtures.{fakeIndividualDetailsDataCache, fakeIndividualDetailsDataCacheWithCRN}
import util.Stubs.userLoggedInFMNUser
import util.TestData.NinoUser
import views.html.print.PrintNationalInsuranceNumberView

import scala.concurrent.{ExecutionContext, Future}

class NinoLetterControllerSpec extends SpecBase with IndividualDetailsFixtures with MockitoSugar {

  override protected def beforeEach(): Unit =
    reset(mockIndividualDetailsService)

  val personDetailsId = "pdId"

  lazy val mockIndividualDetailsService = mock[IndividualDetailsService]
  val mockNPSService                    = mock[NPSService]
  lazy val ninoLetterController         = applicationWithConfig.injector.instanceOf[NinoLetterController]
  lazy val view                         = applicationWithConfig.injector.instanceOf[PrintNationalInsuranceNumberView]

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "NinoLetter Controller" - {
    "must return OK and the correct view for a GET" in {
      userLoggedInFMNUser(NinoUser)
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(
          Future.successful(Right(fakeIndividualDetailsDataCache))
        )

      val application = applicationBuilderWithConfig()
        .overrides(
          bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.NinoLetterController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))

        val result = route(application, request).value
        status(result) mustEqual OK
      }
    }

    "must throw an exception when the individual details cache can't be invalidated" in {
      userLoggedInFMNUser(NinoUser)
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(false))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(
          Future.successful(Right(fakeIndividualDetailsDataCache))
        )

      val application = applicationBuilderWithConfig()
        .overrides(
          bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        assertThrows[RuntimeException] {
          val request = FakeRequest(GET, routes.NinoLetterController.onPageLoad.url)
            .withSession(("authToken", "Bearer 123"))

          val result = route(application, request).value
          status(result)
        }
      }
    }

    "must uplift CRN and return OK and the correct view for a GET" in {
      userLoggedInFMNUser(NinoUser)
      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(
          Future.successful(Right(fakeIndividualDetailsDataCacheWithCRN)),
          Future.successful(Right(fakeIndividualDetailsDataCache))
        )
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))
      when(mockNPSService.upliftCRN(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](true))

      val application = applicationBuilderWithConfig()
        .overrides(
          bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
          bind[NPSService].toInstance(mockNPSService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.NinoLetterController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))

        val result = route(application, request).value
        status(result) mustEqual OK
        verify(mockNPSService, times(1)).upliftCRN(any(), any())(any())
      }
    }
  }

  "NinoLetterController saveNationalInsuranceNumberAsPdf" - {
    "must return OK and pdf file with correct content" in {
      userLoggedInFMNUser(NinoUser)
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(
          Future.successful(Right(fakeIndividualDetailsDataCache))
        )

      val application = applicationBuilderWithConfig()
        .overrides(
          bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.NinoLetterController.saveNationalInsuranceNumberAsPdf.url)
          .withSession(("authToken", "Bearer 123"))

        val result = route(application, request).value
        status(result) mustEqual OK
      }
    }
  }
}
