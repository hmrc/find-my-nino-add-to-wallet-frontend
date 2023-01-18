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

package controllers.auth

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.auth.SignedOutView

import scala.concurrent.Future

class SignedOutControllerSpec extends SpecBase with MockitoSugar {

  "SignedOut Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

      val application =
        applicationBuilder(userAnswers = None)
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.SignedOutController.onPageLoad.url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SignedOutView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}