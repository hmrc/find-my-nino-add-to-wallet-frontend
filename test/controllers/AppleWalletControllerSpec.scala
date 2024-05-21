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
import controllers.auth.requests.UserRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.CDFixtures
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.{NinoUser, NinoUser_With_CL50}
import views.html._

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AppleWalletControllerSpec extends SpecBase with CDFixtures with MockitoSugar {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))

    reset(mockApplePassConnector)
    when(mockApplePassConnector.getApplePass(eqTo(passId))(any(), any()))
      .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))
    when(mockApplePassConnector.createApplePass(any(), any())(any(), any()))
      .thenReturn(Future(Some(passId)))
    when(mockApplePassConnector.getAppleQrCode(eqTo(passId))(any(), any()))
      .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

    reset(mockCitizenDetailsConnector)
    when(mockCitizenDetailsConnector.personDetails(any())(any(), any()))
      .thenReturn(Future(PersonDetailsSuccessResponse(pd)))

    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))

    super.beforeEach()
  }

  val passId = "applePassId"
  val notApplePassId = ""
  val personDetailsId = "pdId"
  val pd = buildPersonDetails
  val controller = applicationWithConfig.injector.instanceOf[AppleWalletController]

  lazy val view = applicationWithConfig.injector.instanceOf[AppleWalletView]
  lazy val errview = applicationWithConfig.injector.instanceOf[ErrorTemplate]
  lazy val redirectview = applicationWithConfig.injector.instanceOf[RedirectToPostalFormView]
  lazy val passIdNotFoundView = applicationWithConfig.injector.instanceOf[PassIdNotFoundView]
  lazy val qrCodeNotFoundView = applicationWithConfig.injector.instanceOf[QRCodeNotFoundView]

  val mockSessionRepository = mock[SessionRepository]
  val mockApplePassConnector = mock[AppleWalletConnector]
  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockIdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]

  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"

  "Apple Wallet Controller" - {

    "Apple wallet enabled" - {
      "must return ErrorView and the correct view for a GET" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
              inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
            )
            .configure(
              "features.apple-wallet-enabled" -> true
            )
            .build()

        when(mockCitizenDetailsConnector.personDetails(any())(any(), any()))
          .thenReturn(Future(PersonDetailsErrorResponse(new RuntimeException("error"))))

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad.url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual INTERNAL_SERVER_ERROR

          contentAsString(result) mustEqual (redirectview()(request, frontendAppConfig, messages(application))).toString()
        }
        reset(mockCitizenDetailsConnector)
      }

      "must return OK and the correct view for a GET" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
            )
            .configure(
              "features.apple-wallet-enabled" -> true
            )
            .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad.url)
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
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
              inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector)
            )
            .configure(
              "features.apple-wallet-enabled" -> true
            )
            .build()

        val view = application.injector.instanceOf[AppleWalletView]

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad.url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual OK
          contentAsString(result) mustEqual (view(passId, false)(request.withAttrs(requestAttributeMap), messages(application))).toString
        }
      }

      "must return apple pass" in {

        val application = applicationBuilderWithConfig().overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> true
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual OK
          contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
        }
      }

      "must redirect to passIdNotFoundView when no Apple pass is returned" in {
        when(mockApplePassConnector.getApplePass(eqTo(passId))(any(), any()))
          .thenReturn(Future(None))

        val application = applicationBuilderWithConfig().overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> true
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          val userRequest = UserRequest(
            None,
            ConfidenceLevel.L200,
            pd,
            Enrolments(Set(Enrolment("HMRC-PT"))),
            request
          )
          contentAsString(result) mustEqual (passIdNotFoundView()(userRequest, frontendAppConfig, messages(application), scala.concurrent.ExecutionContext.global).toString)
        }

      }

      "must return QR code" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> true
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual OK
          contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
        }
      }

      "must redirect to qrCodeNotFoundView when no Apple pass QR code is returned" in {
        when(mockApplePassConnector.getAppleQrCode(eqTo(passId))(any(), any()))
          .thenReturn(Future(None))

        val application = applicationBuilderWithConfig().overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> true
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          val userRequest = UserRequest(
            None,
            ConfidenceLevel.L200,
            pd,
            Enrolments(Set(Enrolment("HMRC-PT"))),
            request
          )
          contentAsString(result) mustEqual (qrCodeNotFoundView()(userRequest, frontendAppConfig, messages(application), scala.concurrent.ExecutionContext.global).toString)
        }
      }

      "must fail to login user" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> true
          )
          .build()

        running(application) {
          userLoggedInIsNotFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual 500
        }
      }

      "must fail to login user2" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> true
          )
          .build()

        running(application) {
          userLoggedInIsNotFMNUser(NinoUser_With_CL50)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual 500
        }
      }
    }

    "Apple wallet disabled" - {
      "must return ErrorView and the correct view for a GET" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
              inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
            )
            .configure(
              "features.apple-wallet-enabled" -> false
            )
            .build()

        when(mockCitizenDetailsConnector.personDetails(any())(any(), any()))
          .thenReturn(Future(PersonDetailsErrorResponse(new RuntimeException("error"))))

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad.url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual INTERNAL_SERVER_ERROR

          contentAsString(result) mustEqual (redirectview()(request, frontendAppConfig, messages(application))).toString()
        }
        reset(mockCitizenDetailsConnector)

      }

      "must return OK and the correct view for a GET" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
            )
            .configure(
              "features.apple-wallet-enabled" -> false
            )
            .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad.url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }

      }

      "must return OK and the correct view for a GET when using the wrapper" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
              inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector)
            )
            .configure(
              "features.apple-wallet-enabled" -> false
            )
            .build()

        val view = application.injector.instanceOf[AppleWalletView]

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad.url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url        }
      }

      "must return apple pass" in {

        val application = applicationBuilderWithConfig().overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual OK
          contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
        }

      }

      "must redirect to passIdNotFoundView when no Apple pass is returned" in {
        when(mockApplePassConnector.getApplePass(eqTo(passId))(any(), any()))
          .thenReturn(Future(None))

        val application = applicationBuilderWithConfig().overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          val userRequest = UserRequest(
            None,
            ConfidenceLevel.L200,
            pd,
            Enrolments(Set(Enrolment("HMRC-PT"))),
            request
          )
          contentAsString(result) mustEqual (passIdNotFoundView()(userRequest, frontendAppConfig, messages(application), scala.concurrent.ExecutionContext.global).toString)
        }
      }

      "must return QR code" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual OK
          contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
        }
      }

      "must redirect to qrCodeNotFoundView when no Apple pass QR code is returned" in {
        when(mockApplePassConnector.getAppleQrCode(eqTo(passId))(any(), any()))
          .thenReturn(Future(None))

        val application = applicationBuilderWithConfig().overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          val userRequest = UserRequest(
            None,
            ConfidenceLevel.L200,
            pd,
            Enrolments(Set(Enrolment("HMRC-PT"))),
            request
          )
          contentAsString(result) mustEqual (qrCodeNotFoundView()(userRequest, frontendAppConfig, messages(application), scala.concurrent.ExecutionContext.global).toString)
        }
      }

      "must fail to login user" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInIsNotFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual 500
        }
      }

      "must fail to login user2" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInIsNotFMNUser(NinoUser_With_CL50)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result = route(application, request).value
          status(result) mustEqual 500
        }

      }
    }

  }
}
