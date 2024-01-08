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
import connectors.{CitizenDetailsConnector, IdentityVerificationFrontendConnector, PayeIndividualDetailsConnector, PersonDetailsErrorResponse, PersonDetailsSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.CDFixtures
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.{NinoUser, NinoUser_With_CL50}
import views.html.{ErrorTemplate, RedirectToPostalFormView, StoreMyNinoView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreMyNinoControllerSpec extends SpecBase with CDFixtures with MockitoSugar {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

    reset(mockCitizenDetailsConnector)
    when(mockCitizenDetailsConnector.personDetails(any())(any()))
      .thenReturn(Future(PersonDetailsSuccessResponse(pd)))

    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))

    super.beforeEach()
  }

  val pd = buildPersonDetails
  val controller = applicationWithConfig.injector.instanceOf[StoreMyNinoController]

  lazy val view = applicationWithConfig.injector.instanceOf[StoreMyNinoView]
  lazy val errview = applicationWithConfig.injector.instanceOf[ErrorTemplate]

  val mockSessionRepository = mock[SessionRepository]
  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockIdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]
  val mockPayeIndividualDetailsConnector = mock[PayeIndividualDetailsConnector]
  lazy val redirectview = applicationWithConfig.injector.instanceOf[RedirectToPostalFormView]

  "StoreMyNino Controller" - {

    "must return ErrorView and the correct view for a GET" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
            inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
          )
          .configure("features.sca-wrapper-enabled" -> false)
          .build()

      when(mockCitizenDetailsConnector.personDetails(any())(any()))
        .thenReturn(Future(PersonDetailsErrorResponse(new RuntimeException("error"))))

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
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
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
            inject.bind[PayeIndividualDetailsConnector].toInstance(mockPayeIndividualDetailsConnector),
          )
          .configure("features.sca-wrapper-enabled" -> false)
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual (view("AA 00 00 03 B")(request, messages(application))).toString
      }
    }

    "must return OK and the correct view for a GET when using the wrapper" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector),
          )
          .configure("features.sca-wrapper-enabled" -> true)
          .build()

      val view = application.injector.instanceOf[StoreMyNinoView]

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad.url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual (view("AA 00 00 03 B")(request.withAttrs(requestAttributeMap), messages(application))).toString
      }
    }

    "must fail to login user" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
        )
        .configure("features.sca-wrapper-enabled" -> false)
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
          inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
        )
        .configure("features.sca-wrapper-enabled" -> true)
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
