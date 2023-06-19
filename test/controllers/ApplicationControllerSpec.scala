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

package controllers

import base.SpecBase
import cats.data.EitherT
import connectors.{ApplePassConnector, CitizenDetailsConnector, IdentityVerificationFrontendConnector, PersonDetailsSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{IdentityVerificationFrontendService, IdentityVerificationResponse, LockedOut, UserAborted}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import util.CDFixtures

import java.util.Base64
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.ErrorTemplate

class ApplicationControllerSpec extends SpecBase with CDFixtures with MockitoSugar {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))

    reset(mockApplePassConnector)
    when(mockApplePassConnector.getApplePass(eqTo(passId))(any(), any()))
      .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))
    when(mockApplePassConnector.createApplePass(any(), any())(any(), any()))
      .thenReturn(Future(Some(passId)))
    when(mockApplePassConnector.createPersonDetailsRow(any())(any(), any()))
      .thenReturn(Future(Some(personDetailsId)))
    when(mockApplePassConnector.getQrCode(eqTo(passId))(any(), any()))
      .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

    reset(mockCitizenDetailsConnector)
    when(mockCitizenDetailsConnector.personDetails(any())(any()))
      .thenReturn(Future(PersonDetailsSuccessResponse(pd)))

    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))

  }

  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  val mockApplePassConnector: ApplePassConnector = mock[ApplePassConnector]
  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockIdentityVerificationFrontendConnector: IdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]
  val mockIdentityVerificationFrontendService: IdentityVerificationFrontendService = mock[IdentityVerificationFrontendService]

  val passId = "applePassId"
  val notApplePassId = ""
  val personDetailsId = "pdId"
  val pd = buildPersonDetails
  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"

  lazy val controller = application.injector.instanceOf[ApplicationController]

  "ApplicationController" - {
    "redirect to StoreMyNinoController on successful uplift" in {

      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[ApplePassConnector].toInstance(mockApplePassConnector),
          inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
          inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
        )
        .configure("features.sca-wrapper-enabled" -> false)
        .build()

      running(application) {
        // val controller = application.injector.instanceOf[ApplicationController]
        val request = FakeRequest("GET", "/uplift")
        val result = controller.uplift(None)(request)
        val expectedRedirectUrl = routes.StoreMyNinoController.onPageLoad.url

        assert(status(result) == SEE_OTHER)
        assert(redirectLocation(result).contains(expectedRedirectUrl))
      }
    }

    /*"showUpliftJourneyOutcome should return Ok when IV journey status is Success" in {

      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[ApplePassConnector].toInstance(mockApplePassConnector),
          inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
          inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
        )
        .configure("features.sca-wrapper-enabled" -> false)
        .build()


      running(application) {

        when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
          .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))
        val request = FakeRequest(GET, "/uplift?journeyId=XXXXX").withSession("sessionId" -> "FAKE_SESSION_ID")
        val result = controller.showUpliftJourneyOutcome(None)(request)

        assert(status(result) == OK)
      }

    }

    "showUpliftJourneyOutcome should return Unauthorized when IV journey status is UserAborted" in {

      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[ApplePassConnector].toInstance(mockApplePassConnector),
          inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
          inject.bind[IdentityVerificationFrontendService].toInstance(mockIdentityVerificationFrontendService)
        )
        .configure("features.sca-wrapper-enabled" -> false)
        .build()



      running(application) {

        when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(UserAborted))))

        val request = FakeRequest(GET, "?journeyId=XXXXX&token=XXXXXX")
        val result = controller.showUpliftJourneyOutcome(None)(request)


        assert(status(result) == UNAUTHORIZED)


      }
*/
    }
  }


    // Add more tests here...


