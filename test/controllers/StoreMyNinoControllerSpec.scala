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
import connectors._
import controllers.auth.requests.UserRequest
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import repositories.SessionRepository
import services.{IndividualDetailsService, NPSService}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.IndividualDetailsFixtures
import util.Fixtures.{fakeIndividualDetailsDataCache, fakeIndividualDetailsDataCacheMissingNinoSuffix, fakeIndividualDetailsDataCacheWithCRN}
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.{NinoUser, NinoUserNoEnrolments, NinoUser_With_CL50, NinoUser_With_Credential_Strength_Weak, trustedHelper, trustedHelperUser}
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
    when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
      .thenReturn(Future.successful(Right(fakeIndividualDetailsDataCache)))

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
      .thenReturn(Future(Some(fakeGooglePassSaveUrl)))
    when(mockGoogleWalletConnector.createGooglePass(any(), any())(any(), any()))
      .thenReturn(Future(Some(googlePassId)))

    super.beforeEach()
  }

  // val pd = buildPersonDetails
  val controller = applicationWithConfig.injector.instanceOf[StoreMyNinoController]

  val googlePassId = "googlePassId"
  val applePassId  = "applePassId"

  val mockAppleWalletConnector  = mock[AppleWalletConnector]
  val mockGoogleWalletConnector = mock[GoogleWalletConnector]

  val mockSessionRepository                     = mock[SessionRepository]
  val mockIndividualDetailsService              = mock[IndividualDetailsService]
  val mockIdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]
  val mockNPSService                            = mock[NPSService]

  val fakeBase64String      = "UEsDBBQACAgIABxqJlYAAAAAAA"
  val fakeGooglePassSaveUrl = "testURL"

  "StoreMyNino Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

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
          fakeIndividualDetailsDataCache,
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
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

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
        userLoggedInFMNUser(trustedHelperUser)
        val request     = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val userRequest = UserRequest(
          None,
          ConfidenceLevel.L200,
          fakeIndividualDetailsDataCache,
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

    "must throw an exception when the individual details cache can't be invalidated" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(false))

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
        userLoggedInFMNUser(NinoUser)

        assertThrows[RuntimeException] {
          val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
            .withSession(("authToken", "Bearer 123"))
          val result  = route(application, request).value
          status(result)
        }

        verify(mockNPSService, times(0)).upliftCRN(any(), any())(any())

      }
    }

    "must redirect to redirect url" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

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
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("Not Found", NOT_FOUND))))

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
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse(" ", UNPROCESSABLE_ENTITY))))

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
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

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
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockGoogleWalletConnector.getGooglePassUrl(eqTo(googlePassId))(any(), any()))
        .thenReturn(Future(None))

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
          fakeIndividualDetailsDataCache,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request,
          None
        )

        contentAsString(result).removeAllNonces() mustEqual (view()(
          userRequest,
          messages(application),
          scala.concurrent.ExecutionContext.global
        ).toString())

      }
    }

    "must return apple pass" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

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
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

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
          fakeIndividualDetailsDataCache,
          Enrolments(Set(Enrolment("HMRC-PT"))),
          request,
          None
        )

        contentAsString(result).removeAllNonces() mustEqual (view()(
          userRequest,
          messages(application),
          scala.concurrent.ExecutionContext.global
        ).toString())

      }

    }

    "must return InternalServerError when Apple pass creation fails" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockAppleWalletConnector.createApplePass(any(), any())(any(), any()))
        .thenReturn(
          EitherT.leftT[Future, HttpResponse](UpstreamErrorResponse("Internal Server Error", INTERNAL_SERVER_ERROR))
        )

      when(mockGoogleWalletConnector.createGooglePass(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(googlePassId)))

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
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(
          Future.successful(Right(fakeIndividualDetailsDataCacheWithCRN)),
          Future.successful(Right(fakeIndividualDetailsDataCache))
        )
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))
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
          fakeIndividualDetailsDataCache,
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

    "must return OK and RedirectToPostalFormView when NINO is missing suffix" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(fakeIndividualDetailsDataCacheMissingNinoSuffix)))

      val app =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[AppleWalletConnector].toInstance(mockAppleWalletConnector),
            inject.bind[GoogleWalletConnector].toInstance(mockGoogleWalletConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService)
          )
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
      }
    }

    "must return OK and RedirectToPostalFormView when CRN uplift is toggled off" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(fakeIndividualDetailsDataCacheWithCRN)))

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

    "must return OK and RedirectToPostalFormView for BAD_REQUEST from CRN uplift" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(fakeIndividualDetailsDataCacheWithCRN)))
      when(mockNPSService.upliftCRN(any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, Boolean](UpstreamErrorResponse("Some error", BAD_REQUEST)))

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

      val view = application.injector.instanceOf[RedirectToPostalFormView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces() mustEqual
          view()(request.withAttrs(requestAttributeMap), frontendAppConfig, messages(application)).toString

        verify(mockNPSService, times(1)).upliftCRN(any(), any())(any())
      }
    }

    "must return OK and RedirectToPostalFormView for UNPROCESSABLE_ENTITY from CRN uplift" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(fakeIndividualDetailsDataCacheWithCRN)))
      when(mockNPSService.upliftCRN(any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, Boolean](UpstreamErrorResponse("Unprocessable Entity", UNPROCESSABLE_ENTITY)))

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

      val view = application.injector.instanceOf[RedirectToPostalFormView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces() mustEqual
          view()(request.withAttrs(requestAttributeMap), frontendAppConfig, messages(application)).toString

        verify(mockNPSService, times(1)).upliftCRN(any(), any())(any())
      }
    }

    "must return OK and RedirectToPostalFormView for NOT_FOUND from CRN uplift" in {
      when(mockIndividualDetailsService.deleteIdDataFromCache(any())(any()))
        .thenReturn(Future.successful(true))

      when(mockIndividualDetailsService.getIdDataFromCache(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(fakeIndividualDetailsDataCacheWithCRN)))
      when(mockNPSService.upliftCRN(any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, Boolean](UpstreamErrorResponse("Not Found", NOT_FOUND)))

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

      val view = application.injector.instanceOf[RedirectToPostalFormView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result  = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces() mustEqual
          view()(request.withAttrs(requestAttributeMap), frontendAppConfig, messages(application)).toString

        verify(mockNPSService, times(1)).upliftCRN(any(), any())(any())
      }
    }
  }
}
