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
import models.individualDetails.IndividualDetails
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.Helpers.*
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import repositories.SessionRepository
import services.{IndividualDetailsService, NPSService}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.Fixtures.{fakeIndividualDetails, fakeindividualDetailsWithCRN}
import util.IndividualDetailsFixtures
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.{NinoUser, NinoUserNoEnrolments, NinoUser_With_CL50, NinoUser_With_Credential_Strength_Weak, trustedHelper}
import views.html.{PassIdNotFoundView, RedirectToPostalFormView, StoreMyNinoView}

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreMyNinoControllerSpec
    extends SpecBase
    with IndividualDetailsFixtures
    with MockitoSugar
    with DefaultAwaitTimeout {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(Some(wrapperDataResponse)))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

    reset(mockIndividualDetailsService)
    when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](fakeIndividualDetails))

    reset(mockNPSService)

    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))

    // mock ApplePass pk-pass and GooglePass URL creation
    reset(mockAppleWalletConnector)
    reset(mockGoogleWalletConnector)

    when(mockAppleWalletConnector.getApplePass(eqTo(applePassId))(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(Base64.getDecoder.decode(fakeBase64String))))

    when(mockAppleWalletConnector.createApplePass(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(applePassId)))

    when(mockGoogleWalletConnector.getGooglePassUrl(eqTo(googlePassId))(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(fakeGooglePassSaveUrl)))
    when(mockGoogleWalletConnector.createGooglePass(any(), any())(any(), any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(googlePassId)))

    super.beforeEach()
  }

  // val pd = buildPersonDetails
  val controller: StoreMyNinoController = applicationWithConfig.injector.instanceOf[StoreMyNinoController]

  val googlePassId = "googlePassId"
  val applePassId  = "applePassId"

  val mockAppleWalletConnector: AppleWalletConnector   = mock[AppleWalletConnector]
  val mockGoogleWalletConnector: GoogleWalletConnector = mock[GoogleWalletConnector]

  val mockSessionRepository: SessionRepository                                         = mock[SessionRepository]
  val mockIndividualDetailsService: IndividualDetailsService                           = mock[IndividualDetailsService]
  val mockIdentityVerificationFrontendConnector: IdentityVerificationFrontendConnector =
    mock[IdentityVerificationFrontendConnector]
  val mockNPSService: NPSService                                                       = mock[NPSService]
  val mockFandFConnector: FandFConnector                                               = mock[FandFConnector]

  val fakeBase64String                                                            = "UEsDBBQACAgIABxqJlYAAAAAAA"
  val fakeGooglePassSaveUrl                                                       = "testURL"
  private val deleteSuccessResponse: EitherT[Future, UpstreamErrorResponse, Unit] =
    EitherT.right(Future.successful((): Unit))
  "StoreMyNino Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

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
        val request     = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result      = route(application, request).value
        status(result) mustEqual OK
        val userRequest = UserRequest(
          None,
          ConfidenceLevel.L200,
          fakeIndividualDetails,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request.withAttrs(requestAttributeMap),
          None
        )

        contentAsString(result).removeAllNonces() mustEqual view(
          applePassId,
          googlePassId,
          "AB 12 34 56 C",
          displayForMobile = false,
          None
        )(userRequest, messages(application)).toString()

        contentAsString(result).removeAllNonces().contains("Save your number to your phone’s wallet") mustBe true

        verify(mockNPSService, times(0)).upliftCRN(any(), any())(any())

      }
    }

    "must return correct view with the trusted helpers displayed and no add to wallet links when loaded by trusted helper" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockFandFConnector.getTrustedHelper()(any())).thenReturn(Future.successful(Some(trustedHelper)))

      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
            inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[FandFConnector].toInstance(mockFandFConnector)
          )
          .build()

      val view = application.injector.instanceOf[StoreMyNinoView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request     = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val userRequest = UserRequest(
          None,
          ConfidenceLevel.L200,
          fakeIndividualDetails,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request.withAttrs(requestAttributeMap),
          Some(trustedHelper)
        )
        val result      = route(application, request).value
        status(result) mustEqual OK

        contentAsString(result).removeAllNonces() mustEqual view(
          applePassId,
          googlePassId,
          "AB 12 34 56 C",
          displayForMobile = false,
          Some(trustedHelper)
        )(userRequest, messages(application)).toString()

        contentAsString(result).removeAllNonces().contains("Save your number to your phone’s wallet") mustBe false

        verify(mockNPSService, times(0)).upliftCRN(any(), any())(any())

      }
    }

    "must redirect to redirect url" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

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

      running(application) {
        userLoggedInFMNUser(NinoUserNoEnrolments)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER

        val target: String =
          "http://localhost:7750/protect-tax-info?redirectUrl=http%3A%2F%2Flocalhost%3A14006%2Fsave-your-national-insurance-number"
        redirectLocation(result) mustEqual Some(target)

        verify(mockNPSService, times(0)).upliftCRN(any(), any())(any())
      }
    }

    "must redirect to error view when individuals details could not be found" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
        .thenReturn(EitherT.leftT[Future, IndividualDetails](UpstreamErrorResponse("Not Found", NOT_FOUND)))

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

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Sorry, the service is unavailable")
      }
    }

    "must redirect to error view when individuals details could not be parsed" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
        .thenReturn(EitherT.leftT[Future, IndividualDetails](UpstreamErrorResponse(" ", UNPROCESSABLE_ENTITY)))

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

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Sorry, the service is unavailable")
        contentAsString(result) must include("To get help with this service, you need to")
        contentAsString(result) must include("contact HMRC")
      }
    }

    "must return google pass" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      val application = applicationBuilderWithConfig()
        .overrides(
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
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustEqual Some(fakeGooglePassSaveUrl)
      }
    }

    "must redirect to passIdNotFoundView when no Google pass is returned" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockGoogleWalletConnector.getGooglePassUrl(eqTo(googlePassId))(any(), any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](None))

      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      val view = application.injector.instanceOf[PassIdNotFoundView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request     = FakeRequest(GET, routes.GoogleWalletController.getGooglePass(googlePassId).url)
          .withSession(("authToken", "Bearer 123"))
        val result      = route(application, request).value
        val userRequest = UserRequest(
          None,
          ConfidenceLevel.L200,
          fakeIndividualDetails,
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

    "must return apple pass" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      val application = applicationBuilderWithConfig()
        .overrides(
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
        val result  = route(application, request).value
        status(result) mustEqual OK
        contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
      }
    }

    "must redirect to passIdNotFoundView when no Apple pass is returned" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockAppleWalletConnector.getApplePass(eqTo(applePassId))(any(), any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](None))

      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      val view = application.injector.instanceOf[PassIdNotFoundView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request     = FakeRequest(GET, routes.AppleWalletController.getPassCard(applePassId).url)
          .withSession(("authToken", "Bearer 123"))
        val result      = route(application, request).value
        val userRequest = UserRequest(
          None,
          ConfidenceLevel.L200,
          fakeIndividualDetails,
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

    "must return InternalServerError when Apple pass creation fails" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockAppleWalletConnector.createApplePass(any(), any())(any(), any()))
        .thenReturn(
          EitherT.leftT[Future, HttpResponse](UpstreamErrorResponse("Internal Server Error", INTERNAL_SERVER_ERROR))
        )

      when(mockGoogleWalletConnector.createGooglePass(any(), any())(any(), any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(googlePassId)))

      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))

        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Error: Internal Server Error")
      }
    }

    "must return InternalServerError when Google Pass creation fails" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockAppleWalletConnector.createApplePass(any(), any())(any(), any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Some(applePassId)))

      when(mockGoogleWalletConnector.createGooglePass(any(), any())(any(), any()))
        .thenReturn(
          EitherT.leftT[Future, HttpResponse](UpstreamErrorResponse("Internal Server Error", INTERNAL_SERVER_ERROR))
        )

      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))

        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Error: Internal Server Error")
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
        val result  = route(application, request).value
        status(result) mustEqual 500
      }
    }

    "must return INTERNAL_SERVER_ERROR when auth returns an unauthorized" in {
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
        val result  = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR
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
        userLoggedInFMNUser(NinoUser_With_CL50)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
      }
    }

    "must fail to login user with weak credential strength" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
          inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
          inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
        )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser_With_Credential_Strength_Weak)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(application, request).value
        status(result) mustEqual SEE_OTHER
      }
    }

    "must uplift a CRN when CRN uplift is toggled on" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
        .thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](fakeindividualDetailsWithCRN),
          EitherT.rightT[Future, UpstreamErrorResponse](fakeIndividualDetails)
        )
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)
      when(mockNPSService.upliftCRN(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](true))

      val app =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
            inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[NPSService].toInstance(mockNPSService)
          )
          .configure("features.crn-upgrade-enabled" -> true)
          .build()

      val view = app.injector.instanceOf[StoreMyNinoView]

      running(app) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(app, request).value

        val userRequest = UserRequest(
          None,
          ConfidenceLevel.L200,
          fakeIndividualDetails,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request.withAttrs(requestAttributeMap),
          None
        )

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces() mustEqual view(
          applePassId,
          googlePassId,
          "AB 12 34 56 C",
          displayForMobile = false,
          None
        )(userRequest, messages(app)).toString()
        verify(mockNPSService, times(1)).upliftCRN(any(), any())(any())
      }
    }

    "must return OK and RedirectToPostalFormView when CRN uplift is toggled off" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](fakeindividualDetailsWithCRN))

      val app =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
            inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
          .configure("features.crn-uplift-enabled" -> false)
          .build()

      val view = app.injector.instanceOf[RedirectToPostalFormView]

      running(app) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(app, request).value

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces() mustEqual
          view()(request.withAttrs(requestAttributeMap), frontendAppConfig, messages(app)).toString

        verify(mockNPSService, times(0)).upliftCRN(any(), any())(any())
      }
    }

    "must return INTERNAL_SERVER_ERROR with error view for Left(UpstreamErrorResponse) from CRN uplift" in {
      when(mockIndividualDetailsService.deleteIdData(any())(any(), any()))
        .thenReturn(deleteSuccessResponse)

      when(mockIndividualDetailsService.getIdData(any(), any())(any(), any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](fakeindividualDetailsWithCRN))
      when(mockNPSService.upliftCRN(any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, Boolean](UpstreamErrorResponse(" ", INTERNAL_SERVER_ERROR)))

      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
            inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[NPSService].toInstance(mockNPSService)
          )
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
        contentAsString(result) must include("Sorry, the service is unavailable")

        verify(mockNPSService, times(1)).upliftCRN(any(), any())(any())
      }
    }
  }
}
