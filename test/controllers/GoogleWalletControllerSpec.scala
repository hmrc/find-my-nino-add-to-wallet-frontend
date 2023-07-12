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
import connectors.{CitizenDetailsConnector, IdentityVerificationFrontendConnector, PersonDetailsErrorResponse, PersonDetailsSuccessResponse, StoreMyNinoConnector}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.{CDFixtures, GoogleCredentialsHelper, Keys}
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.NinoUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import views.html.{ErrorTemplate, GoogleWalletView}

import java.util.Base64

class GoogleWalletControllerSpec extends SpecBase with CDFixtures with MockitoSugar {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))

    reset(mockApplePassConnector)
    when(mockApplePassConnector.getGooglePassUrl(eqTo(passId))(any(), any()))
      .thenReturn(Future(Some(fakeGooglePassSaveUrl)))
    when(mockApplePassConnector.createGooglePassWithCredentials(any(), any(), any())(any(), any()))
      .thenReturn(Future(Some(passId)))
    when(mockApplePassConnector.createPersonDetailsRow(any())(any(), any()))
      .thenReturn(Future(Some(personDetailsId)))
    when(mockApplePassConnector.getGooglePassQrCode(eqTo(passId))(any(), any()))
      .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

    reset(mockCitizenDetailsConnector)
    when(mockCitizenDetailsConnector.personDetails(any())(any()))
      .thenReturn(Future(PersonDetailsSuccessResponse(pd)))

    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))


    reset(mockGoogleCredentialsHelper)
    when(mockGoogleCredentialsHelper.createGoogleCredentials(any()))
      .thenReturn(passId)

    super.beforeEach()
  }

  val passId = "googlePassId"
  val pd = buildPersonDetails
  val personDetailsId = "pdId"
  val controller = applicationWithConfig.injector.instanceOf[GoogleWalletController]

  lazy val view = applicationWithConfig.injector.instanceOf[GoogleWalletView]
  lazy val errview = applicationWithConfig.injector.instanceOf[ErrorTemplate]

  val mockSessionRepository = mock[SessionRepository]
  val mockApplePassConnector = mock[StoreMyNinoConnector]
  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockIdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]
  val mockGoogleCredentialsHelper = mock[GoogleCredentialsHelper]


  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"
  val fakeGooglePassSaveUrl = "testURL"

  "Google Wallet Controller" - {

    "must return ErrorView and the correct view for a GET" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
            inject.bind[StoreMyNinoConnector].toInstance(mockApplePassConnector),
            inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
          )
          .configure("features.sca-wrapper-enabled" -> false)
          .build()

      when(mockCitizenDetailsConnector.personDetails(any())(any()))
        .thenReturn(Future(PersonDetailsErrorResponse(new RuntimeException("error"))))

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual NOT_FOUND

        contentAsString(result) mustEqual (errview("Details not found",
          "Your details were not found.", "Your details were not found, please try again later.")(request, messages(application))).toString
      }
      reset(mockCitizenDetailsConnector)
    }


    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[StoreMyNinoConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
            inject.bind[GoogleCredentialsHelper].toInstance(mockGoogleCredentialsHelper)
          )
          .configure("features.sca-wrapper-enabled" -> false)
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.onPageLoad().url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual (view(passId, false)(request, messages(application))).toString
      }
    }

    "must return OK and the correct view for a GET when using the wrapper" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[StoreMyNinoConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
            inject.bind[GoogleCredentialsHelper].toInstance(mockGoogleCredentialsHelper)
          )
          .configure("features.sca-wrapper-enabled" -> true)
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GoogleWalletController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual (view(passId, false)(request.addAttr(Keys.wrapperDataKey, wrapperDataResponse), messages(application))).toString
      }
    }

    "must return google pass" in {

      val application = applicationBuilderWithConfig().overrides(
        inject.bind[SessionRepository].toInstance(mockSessionRepository),
        inject.bind[StoreMyNinoConnector].toInstance(mockApplePassConnector),
        inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
      )
        .configure("features.sca-wrapper-enabled" -> false)
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

    "must return QR code" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[StoreMyNinoConnector].toInstance(mockApplePassConnector),
          inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
        )
        .configure("features.sca-wrapper-enabled" -> false)
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

    "must fail to login user" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[StoreMyNinoConnector].toInstance(mockApplePassConnector),
          inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
        )
        .configure("features.sca-wrapper-enabled" -> false)
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