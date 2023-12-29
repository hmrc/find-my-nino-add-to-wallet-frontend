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
import connectors._
import models.GovUkPassCreateResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.NinoUser
import util.CDFixtures
import views.html.{ErrorTemplate, GovUKWalletView, RedirectToPostalFormView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GovUKWalletControllerSpec extends SpecBase with CDFixtures with MockitoSugar {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))

    reset(mockGovUKWalletSMNConnector)
    when(mockGovUKWalletSMNConnector.createGovUKPass(any(), any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(GovUkPassCreateResponse("passId", "qrCodeImage"))))

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

  val passId = "govukWalletPassId"
  val pd = buildPersonDetails
  val personDetailsId = "pdId"
  val controller = applicationWithConfig.injector.instanceOf[GovUKWalletController]

  lazy val view = applicationWithConfig.injector.instanceOf[GovUKWalletView]
  lazy val errview = applicationWithConfig.injector.instanceOf[ErrorTemplate]

  val mockSessionRepository = mock[SessionRepository]
  val mockGovUKWalletSMNConnector = mock[GovUKWalletSMNConnector]
  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockIdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]
  lazy val redirectview = applicationWithConfig.injector.instanceOf[RedirectToPostalFormView]


  "Govuk Wallet Controller" - {

    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[GovUKWalletSMNConnector].toInstance(mockGovUKWalletSMNConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .configure("features.sca-wrapper-enabled" -> false)
          .configure("features.govuk-wallet-enabled" -> true)
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GovUKWalletController.onPageLoad().url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual (view(passId, "qrCodeImage", false)(request, messages(application))).toString
      }
    }

    "must return OK and the correct view for a GET when using the wrapper" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[GovUKWalletSMNConnector].toInstance(mockGovUKWalletSMNConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector)
          )
          .configure("features.sca-wrapper-enabled" -> true)
          .configure("features.govuk-wallet-enabled" -> true)
          .build()

      val view = application.injector.instanceOf[GovUKWalletView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GovUKWalletController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual (view(passId, "qrCodeImage", false)(request.withAttrs(requestAttributeMap), messages(application))).toString
      }
    }

    "must fail to login user" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[GovUKWalletSMNConnector].toInstance(mockGovUKWalletSMNConnector),
          inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
        )
        .configure("features.sca-wrapper-enabled" -> false)
        .configure("features.govuk-wallet-enabled" -> true)
        .build()

      running(application) {
        userLoggedInIsNotFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GovUKWalletController.onPageLoad().url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual 500
      }
    }

    "must redirect to unauthorised view when govuk wallet toggle disabled" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[GovUKWalletSMNConnector].toInstance(mockGovUKWalletSMNConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector)
          )
          .configure("features.sca-wrapper-enabled" -> true)
          .configure("features.govuk-wallet-enabled" -> false)
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.GovUKWalletController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

  }
}