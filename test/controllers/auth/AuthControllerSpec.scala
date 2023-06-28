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
import controllers.bindable.Origin
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.concurrent.Future

class AuthControllerSpec extends SpecBase with MockitoSugar {

  "signOut" - {

    "must clear user answers and redirect to sign out, specifying the exit survey as the continue URL when the sca wrapper is enabled" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .configure("features.sca-wrapper-enabled" -> true)
          .build()

      running(application) {

        val sentLocation = "http://example.com&origin=STORE_MY_NINO"
        val request   = FakeRequest(GET, routes.AuthController.signout(Some(RedirectUrl(sentLocation)),Some(Origin("STORE_MY_NINO"))).url)

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER

      }
    }

    "must clear user answers and redirect to sign out, specifying the exit survey as the continue URL when the sca wrapper is disabled" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .configure("features.sca-wrapper-enabled" -> false)
          .build()

      running(application) {

        val sentLocation = "http://example.com&origin=STORE_MY_NINO"
        val request = FakeRequest(GET, routes.AuthController.signout(Some(RedirectUrl(sentLocation)), Some(Origin("STORE_MY_NINO"))).url)

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER

      }
    }

    "must not redirect when origin is missing" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(None)
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .configure("features.sca-wrapper-enabled" -> false)
          .build()

      running(application) {

        val sentLocation = "http://example.com&origin=STORE_MY_NINO"
        val request = FakeRequest(GET, routes.AuthController.signout(Some(RedirectUrl(sentLocation)), None).url)

        val result = route(application, request).value


        status(result) mustEqual SEE_OTHER

      }
    }
  }
}
