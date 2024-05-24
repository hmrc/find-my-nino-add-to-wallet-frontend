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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.{JourneyRecoveryContinueView, JourneyRecoveryStartAgainView}

import scala.concurrent.Future

class JourneyRecoveryControllerSpec extends SpecBase with MockitoSugar {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))
    super.beforeEach()
  }

  "JourneyRecovery Controller" - {

    "when a relative continue Url is supplied" - {

      "must return OK and the continue view" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder(userAnswers = None)
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
            )

            .build()

        running(application) {
          val continueUrl = RedirectUrl("/foo")
          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val continueView = application.injector.instanceOf[JourneyRecoveryContinueView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual continueView(continueUrl.unsafeValue)(request, messages(application)).toString
        }
      }
    }

    "when an absolute continue Url is supplied" - {

      "must return OK and the start again view" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder(userAnswers = None)
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
            )

            .build()

        running(application) {
          val continueUrl = RedirectUrl("https://foo.com")
          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual startAgainView()(request, messages(application)).toString
        }
      }
    }

    "when no continue Url is supplied" - {

      "must return OK and the start again view" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder(userAnswers = None)
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository),
            )

            .build()

        running(application) {
          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual startAgainView()(request, messages(application)).toString
        }
      }
    }
  }
}
