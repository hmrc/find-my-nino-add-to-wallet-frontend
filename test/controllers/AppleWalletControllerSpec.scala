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
import connectors.*
import controllers.auth.requests.UserRequest
import models.individualDetails.IndividualDetailsDataCache
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.IndividualDetailsService
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.Fixtures.{fakeIndividualDetailsDataCache, fakeIndividualDetailsDataCacheNoAddress}
import util.IndividualDetailsFixtures
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.{NinoUser, NinoUser_With_CL50, trustedHelper}
import views.html.*

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AppleWalletControllerSpec extends SpecBase with IndividualDetailsFixtures with MockitoSugar {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(Some(wrapperDataResponse)))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))

    reset(mockApplePassConnector)
    when(mockApplePassConnector.getApplePass(eqTo(passId))(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(Base64.getDecoder.decode(fakeBase64String))))

    when(mockApplePassConnector.createApplePass(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(passId)))

    when(mockApplePassConnector.getAppleQrCode(eqTo(passId))(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(Base64.getDecoder.decode(fakeBase64String))))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

    reset(mockIndividualDetailsService)
    when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](fakeIndividualDetailsDataCache))

    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))

    super.beforeEach()
  }

  val passId           = "applePassId"
  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"

  val controller: AppleWalletController = applicationWithConfig.injector.instanceOf[AppleWalletController]

  val mockSessionRepository: SessionRepository                                         = mock[SessionRepository]
  val mockApplePassConnector: AppleWalletConnector                                     = mock[AppleWalletConnector]
  val mockIndividualDetailsService: IndividualDetailsService                           = mock[IndividualDetailsService]
  val mockIdentityVerificationFrontendConnector: IdentityVerificationFrontendConnector =
    mock[IdentityVerificationFrontendConnector]
  val mockFandFConnector: FandFConnector                                               = mock[FandFConnector]

  "Apple Wallet Controller" - {

    "Apple wallet enabled" - {

      "must redirect to error view when ID cache is not found" in {

        when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
          .thenReturn(EitherT.leftT[Future, IndividualDetailsDataCache](UpstreamErrorResponse("Not Found", NOT_FOUND)))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
          )
          .configure("features.apple-wallet-enabled" -> true, "features.crn-upgrade-enabled" -> true)
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad().url)
            .withSession(("authToken", "Bearer 123"))

          val result = route(application, request).value

          status(result) mustEqual INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Sorry, the service is unavailable")
        }
      }

      "must return OK and the correct view for a GET" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
              inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
            )
            .configure(
              Map("features.apple-wallet-enabled" -> true, "features.crn-upgrade-enabled" -> true)
            )
            .build()

        val view = application.injector.instanceOf[AppleWalletView]

        when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](fakeIndividualDetailsDataCache))

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad().url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual OK
          contentAsString(result).removeAllNonces() mustEqual view(passId, false)(
            request.withAttrs(requestAttributeMap),
            messages(application)
          ).toString()
        }
      }

      "must return OK and the correct view for a GET with no address on individual details" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
              inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
            )
            .configure(
              "features.apple-wallet-enabled" -> true,
              "features.crn-upgrade-enabled"  -> true
            )
            .build()

        val view = application.injector.instanceOf[AppleWalletView]

        when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](fakeIndividualDetailsDataCacheNoAddress))

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad().url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual OK
          contentAsString(result).removeAllNonces() mustEqual view(passId, false)(
            request.withAttrs(requestAttributeMap),
            messages(application)
          ).toString()
        }
      }

      "must return apple pass" in {

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> true,
            "features.crn-upgrade-enabled"  -> true
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual OK
          contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
        }
      }

      "must redirect to passIdNotFoundView when no Apple pass is returned" in {
        when(mockApplePassConnector.getApplePass(eqTo(passId))(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](None))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> true,
            "features.crn-upgrade-enabled"  -> true
          )
          .build()

        val view = application.injector.instanceOf[PassIdNotFoundView]

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request     = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result      = route(application, request).value
          val userRequest = UserRequest(
            None,
            ConfidenceLevel.L200,
            fakeIndividualDetailsDataCache,
            Enrolments(Set(Enrolment("HMRC-PT"))),
            request,
            None
          )

          contentAsString(result).removeAllNonces() mustEqual view()(
            userRequest,
            messages(application),
            scala.concurrent.ExecutionContext.global
          ).toString()

        }

      }

      "must return InternalServerError when Apple Pass retrieval fails" in {
        when(mockApplePassConnector.getApplePass(eqTo(passId))(any(), any()))
          .thenReturn(EitherT.leftT[Future, HttpResponse](UpstreamErrorResponse("some error", INTERNAL_SERVER_ERROR)))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure("features.apple-wallet-enabled" -> true, "features.crn-upgrade-enabled" -> true)
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Failed to get Apple Pass: some error")
        }
      }

      "must return QR code" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure("features.apple-wallet-enabled" -> true, "features.crn-upgrade-enabled" -> true)
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual OK
          contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
        }
      }

      "must redirect to qrCodeNotFoundView when no Apple pass QR code is returned" in {
        when(mockApplePassConnector.getAppleQrCode(eqTo(passId))(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](None))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> true,
            "features.crn-upgrade-enabled"  -> true
          )
          .build()

        val view = application.injector.instanceOf[QRCodeNotFoundView]

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request     = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result      = route(application, request).value
          val userRequest = UserRequest(
            None,
            ConfidenceLevel.L200,
            fakeIndividualDetailsDataCache,
            Enrolments(Set(Enrolment("HMRC-PT"))),
            request,
            None
          )

          contentAsString(result).removeAllNonces() mustEqual view()(
            userRequest,
            messages(application),
            scala.concurrent.ExecutionContext.global
          ).toString()

        }
      }

      "must return InternalServerError when Apple QR Code retrieval fails" in {
        when(mockApplePassConnector.getAppleQrCode(eqTo(passId))(any(), any()))
          .thenReturn(EitherT.leftT[Future, HttpResponse](UpstreamErrorResponse("some error", INTERNAL_SERVER_ERROR)))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure("features.apple-wallet-enabled" -> true, "features.crn-upgrade-enabled" -> true)
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Failed to get Apple QR Code: some error")
        }
      }

      "must fail to login user" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> true,
            "features.crn-upgrade-enabled"  -> true
          )
          .build()

        running(application) {
          userLoggedInIsNotFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual 500
        }
      }

      "must fail to login user2" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> true,
            "features.crn-upgrade-enabled"  -> true
          )
          .build()

        running(application) {
          userLoggedInIsNotFMNUser(NinoUser_With_CL50)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual 500
        }
      }
      "redirect to Store my Nino home page when trusted helper user tries to access this page" in {
        when(mockFandFConnector.getTrustedHelper()(any())).thenReturn(Future.successful(Some(trustedHelper)))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[FandFConnector].toInstance(mockFandFConnector)
          )
          .configure("features.google-wallet-enabled" -> true, "features.crn-upgrade-enabled" -> true)
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad().url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual 303
          redirectLocation(result) mustEqual Some(controllers.routes.StoreMyNinoController.onPageLoad.toString)
        }
      }
    }

    "Apple wallet disabled" - {

      "must redirect to error view when ID cache is not found" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
              inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
            )
            .configure("features.apple-wallet-enabled" -> true, "features.crn-upgrade-enabled" -> true)
            .build()

        when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
          .thenReturn(EitherT.leftT[Future, IndividualDetailsDataCache](UpstreamErrorResponse("Not Found", NOT_FOUND)))

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad().url)
            .withSession(("authToken", "Bearer 123"))

          val result = route(application, request).value

          status(result) mustEqual INTERNAL_SERVER_ERROR
          contentAsString(result) must include("Sorry, the service is unavailable")
        }
      }

      "must return OK and the correct view for a GET" in {
        val application =
          applicationBuilderWithConfig()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
              inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
              inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
              inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
            )
            .configure("features.apple-wallet-enabled" -> false)
            .build()

        when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](fakeIndividualDetailsDataCache))

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad().url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }

      "must return apple pass" in {

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure("features.apple-wallet-enabled" -> false)
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual OK
          contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
        }
      }

      "must redirect to passIdNotFoundView when no Apple pass is returned" in {
        when(mockApplePassConnector.getApplePass(eqTo(passId))(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](None))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure("features.apple-wallet-enabled" -> false)
          .build()

        val view = application.injector.instanceOf[PassIdNotFoundView]

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request     = FakeRequest(GET, routes.AppleWalletController.getPassCard(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result      = route(application, request).value
          val userRequest = UserRequest(
            None,
            ConfidenceLevel.L200,
            fakeIndividualDetailsDataCache,
            Enrolments(Set(Enrolment("HMRC-PT"))),
            request,
            None
          )

          contentAsString(result).removeAllNonces() mustEqual view()(
            userRequest,
            messages(application),
            scala.concurrent.ExecutionContext.global
          ).toString()

        }

      }

      "must return QR code" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual OK
          contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
        }
      }

      "must redirect to qrCodeNotFoundView when no Apple pass QR code is returned" in {
        when(mockApplePassConnector.getAppleQrCode(eqTo(passId))(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](None))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        val view = application.injector.instanceOf[QRCodeNotFoundView]

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request     = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result      = route(application, request).value
          val userRequest = UserRequest(
            None,
            ConfidenceLevel.L200,
            fakeIndividualDetailsDataCache,
            Enrolments(Set(Enrolment("HMRC-PT"))),
            request,
            None
          )

          contentAsString(result).removeAllNonces() mustEqual view()(
            userRequest,
            messages(application),
            scala.concurrent.ExecutionContext.global
          ).toString()

        }
      }

      "must fail to login user" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInIsNotFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual 500
        }
      }

      "must fail to login user2" in {
        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure(
            "features.apple-wallet-enabled" -> false
          )
          .build()

        running(application) {
          userLoggedInIsNotFMNUser(NinoUser_With_CL50)
          val request = FakeRequest(GET, routes.AppleWalletController.getQrCode(passId).url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual 500
        }
      }
      "redirect to Store my Nino home page when trusted helper user tries to access this page" in {
        when(mockFandFConnector.getTrustedHelper()(any())).thenReturn(Future.successful(Some(trustedHelper)))

        val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockApplePassConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[FandFConnector].toInstance(mockFandFConnector)
          )
          .configure("features.google-wallet-enabled" -> true, "features.crn-upgrade-enabled" -> true)
          .build()

        running(application) {
          userLoggedInFMNUser(NinoUser)
          val request = FakeRequest(GET, routes.AppleWalletController.onPageLoad().url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result) mustEqual 303
          redirectLocation(result) mustEqual Some(controllers.routes.StoreMyNinoController.onPageLoad.toString)
        }
      }
    }
  }
}
