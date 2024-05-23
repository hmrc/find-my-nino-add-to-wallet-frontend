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

package controllers

import base.SpecBase
import connectors._
import controllers.auth.requests.UserRequestNew
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers._
import repositories.SessionRepository
import services.IndividualDetailsService
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.CDFixtures
import util.Fixtures.{fakeIndividualDetails, fakeIndividualDetailsData, fakeIndividualDetailsDataCache}
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.{NinoUser, NinoUser_With_CL50}
import views.html.{ErrorTemplate, PassIdNotFoundView, RedirectToPostalFormView, StoreMyNinoView}

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreMyNinoControllerSpec extends SpecBase with CDFixtures with MockitoSugar with DefaultAwaitTimeout {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))


    reset(mockIndividualDetailsService)
    when(mockIndividualDetailsService.getIdDataFromCache(any(),any())(any(),any()))
      .thenReturn(Future.successful(Right(fakeIndividualDetailsDataCache)))


    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))

    //mock ApplePass pk-pass and GooglePass URL creation
    reset(mockAppleWalletConnector)
    reset(mockGoogleWalletConnector)

    when(mockAppleWalletConnector.getApplePass(eqTo(applePassId))(any(), any()))
      .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))
    when(mockAppleWalletConnector.createApplePass(any(), any())(any(), any()))
      .thenReturn(Future(Some(applePassId)))

    when(mockGoogleWalletConnector.getGooglePassUrl(eqTo(googlePassId))(any(), any()))
      .thenReturn(Future(Some(fakeGooglePassSaveUrl)))
    when(mockGoogleWalletConnector.createGooglePass(any(), any())(any(), any()))
      .thenReturn(Future(Some(googlePassId)))

    super.beforeEach()
  }

  //val pd = buildPersonDetails
  val controller = applicationWithConfig.injector.instanceOf[StoreMyNinoController]

  val googlePassId = "googlePassId"
  val applePassId = "applePassId"

  val mockAppleWalletConnector = mock[AppleWalletConnector]
  val mockGoogleWalletConnector = mock[GoogleWalletConnector]

  val mockSessionRepository = mock[SessionRepository]
  val mockIndividualDetailsService = mock[IndividualDetailsService]
  val mockIdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]

  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"
  val fakeGooglePassSaveUrl = "testURL"

  "StoreMyNino Controller" - {

    "must return ErrorView and the correct view for a GET" in {

      reset(mockIndividualDetailsService)
      when(mockIndividualDetailsService.getIdDataFromCache(any(),any())(any(),any()))
        .thenReturn(Future.successful(Left("Individual details not found in cache")))

      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
            inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
            inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
          )
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        assertThrows[NotFoundException] {
          await(route(application, request).value)
        }
      }
      reset(mockIndividualDetailsService)
    }

    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
            inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .build()

      val view = application.injector.instanceOf[StoreMyNinoView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(applePassId, googlePassId,"AA 00 00 03 B", displayForMobile = false)(request.withAttrs(requestAttributeMap), messages(application)).toString
      }
    }

    "must return google pass" in {
      val application = applicationBuilderWithConfig().overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.getGooglePass(googlePassId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustEqual Some(fakeGooglePassSaveUrl)
      }
    }

    "must redirect to passIdNotFoundView when no Google pass is returned" in {
      when(mockGoogleWalletConnector.getGooglePassUrl(eqTo(googlePassId))(any(), any()))
        .thenReturn(Future(None))

      val application = applicationBuilderWithConfig().overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      val view = application.injector.instanceOf[PassIdNotFoundView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.getGooglePass(googlePassId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        val userRequest = UserRequestNew(
          None,
          ConfidenceLevel.L200,
          fakeIndividualDetailsDataCache,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request
        )
        contentAsString(result) mustEqual (view()(userRequest, messages(application), scala.concurrent.ExecutionContext.global).toString)
      }
    }

    "must return apple pass" in {
      val application = applicationBuilderWithConfig().overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(applePassId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
      }
    }

    "must redirect to passIdNotFoundView when no Apple pass is returned" in {
      when(mockAppleWalletConnector.getApplePass(eqTo(applePassId))(any(), any()))
        .thenReturn(Future(None))

      val application = applicationBuilderWithConfig().overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      val view = application.injector.instanceOf[PassIdNotFoundView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(applePassId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        val userRequest = UserRequestNew(
          None,
          ConfidenceLevel.L200,
          fakeIndividualDetailsDataCache,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request
        )
        contentAsString(result) mustEqual (view()(userRequest, messages(application), scala.concurrent.ExecutionContext.global).toString)
      }

    }

    "must fail to login user" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInIsNotFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual 500
      }
    }

    "must fail to login user with 50 CL" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInIsNotFMNUser(NinoUser_With_CL50)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual 500
      }
    }
  }
}