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
import connectors.{GoogleWalletConnector, IdentityVerificationFrontendConnector}
import controllers.auth.requests.UserRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.IndividualDetailsService
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.CDFixtures
import util.Fixtures.fakeIndividualDetailsDataCache
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.NinoUser
import views.html._

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GoogleWalletControllerSpec extends SpecBase with CDFixtures with MockitoSugar {


  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))

    reset(mockGooglePassConnector)
    when(mockGooglePassConnector.getGooglePassUrl(eqTo(passId))(any(), any()))
      .thenReturn(Future(Some(fakeGooglePassSaveUrl)))
    when(mockGooglePassConnector.createGooglePass(any(), any())(any(), any()))
      .thenReturn(Future(Some(passId)))
    when(mockGooglePassConnector.getGooglePassQrCode(eqTo(passId))(any(), any()))
      .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

    reset(mockIndividualDetailsService)
    when(mockIndividualDetailsService.getIdDataFromCache(any(),any())(any(),any()))
      .thenReturn(Future.successful(Right(fakeIndividualDetailsDataCache)))

    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))


    super.beforeEach()
  }

  val passId = "googlePassId"
  val pd = buildPersonDetails
  val personDetailsId = "pdId"
  val controller = applicationWithConfig.injector.instanceOf[GoogleWalletController]

  lazy val view = applicationWithConfig.injector.instanceOf[GoogleWalletView]
  lazy val errview = applicationWithConfig.injector.instanceOf[ErrorTemplate]

  val mockSessionRepository = mock[SessionRepository]
  val mockGooglePassConnector = mock[GoogleWalletConnector]
  val mockIndividualDetailsService = mock[IndividualDetailsService]
  val mockIdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]
  lazy val redirectview = applicationWithConfig.injector.instanceOf[RedirectToPostalFormView]
  lazy val passIdNotFoundView = applicationWithConfig.injector.instanceOf[PassIdNotFoundView]
  lazy val qrCodeNotFoundView = applicationWithConfig.injector.instanceOf[QRCodeNotFoundView]

  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"
  val fakeGooglePassSaveUrl = "testURL"

  "Google Wallet Controller" - {

    "must return ErrorView and the correct view for a GET" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[GoogleWalletConnector].toInstance(mockGooglePassConnector),
            inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
          )
          .build()

      when(mockIndividualDetailsService.getIdDataFromCache(any(),any())(any(),any()))
        .thenReturn(Future.successful(Left("Individual details not found in cache")))

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        reset(mockIndividualDetailsService)
      }
    }

    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[GoogleWalletConnector].toInstance(mockGooglePassConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.onPageLoad().url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual (view(passId, false)(request.withAttrs(requestAttributeMap), messages(application))).toString
      }
    }

    "must return google pass" in {

      val application = applicationBuilderWithConfig().overrides(
        inject.bind[SessionRepository].toInstance(mockSessionRepository),
        inject.bind[GoogleWalletConnector].toInstance(mockGooglePassConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
      )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.getGooglePass(passId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual 303
        redirectLocation(result) mustEqual Some(fakeGooglePassSaveUrl)
      }
    }

    "must redirect to passIdNotFoundView when no Google pass is returned" in {
      when(mockGooglePassConnector.getGooglePassUrl(eqTo(passId))(any(), any()))
        .thenReturn(Future(None))

      val application = applicationBuilderWithConfig().overrides(
        inject.bind[SessionRepository].toInstance(mockSessionRepository),
        inject.bind[GoogleWalletConnector].toInstance(mockGooglePassConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
      )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.getGooglePass(passId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        val userRequest = UserRequest(
          None,
          ConfidenceLevel.L200,
          pd,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request
        )
        contentAsString(result) mustEqual (passIdNotFoundView()(userRequest, messages(application), scala.concurrent.ExecutionContext.global).toString)
      }
    }

    "must return QR code" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[GoogleWalletConnector].toInstance(mockGooglePassConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.getGooglePassQrCode(passId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
      }
    }

    "must redirect to qrCodeNotFoundView when no Google pass QR code is returned" in {
      when(mockGooglePassConnector.getGooglePassQrCode(eqTo(passId))(any(), any()))
        .thenReturn(Future(None))

      val application = applicationBuilderWithConfig().overrides(
        inject.bind[SessionRepository].toInstance(mockSessionRepository),
        inject.bind[GoogleWalletConnector].toInstance(mockGooglePassConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
      )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.getGooglePassQrCode(passId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        val userRequest = UserRequest(
          None,
          ConfidenceLevel.L200,
          pd,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request
        )
        contentAsString(result) mustEqual (qrCodeNotFoundView()(userRequest, messages(application), scala.concurrent.ExecutionContext.global).toString)
      }
    }

    "must fail to login user" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[GoogleWalletConnector].toInstance(mockGooglePassConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInIsNotFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.onPageLoad().url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual 500
      }
    }
  }
}