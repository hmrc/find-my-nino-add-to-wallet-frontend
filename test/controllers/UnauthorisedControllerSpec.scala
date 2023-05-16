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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import views.html.UnauthorisedView

import scala.concurrent.Future

class UnauthorisedControllerSpec extends SpecBase with MockitoSugar {
  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))
    when(mockScaWrapperDataConnector.messageData()(any(), any()))
      .thenReturn(Future.successful(messageDataResponse))
    super.beforeEach()
  }

  "Unauthorised Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
          )
          .configure("features.sca-wrapper-enabled" -> false)
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.UnauthorisedController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UnauthorisedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when using the wrapper" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[ScaWrapperDataConnector].toInstance(mockScaWrapperDataConnector)
          )
          .configure("features.sca-wrapper-enabled" -> true)
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.UnauthorisedController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UnauthorisedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request.withAttrs(requestAttributeMap), messages(application)).toString
      }
    }
  }
}
