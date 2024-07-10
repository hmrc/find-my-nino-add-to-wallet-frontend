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
import uk.gov.hmrc.sca.services.WrapperService

import scala.concurrent.Future

class AuthControllerSpec extends SpecBase with MockitoSugar {

  "signOut" - {

    "must clear user answers and redirect to sign out" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder()
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {

        val sentLocation = "http://example.com&origin=STORE_MY_NINO"
        val request = FakeRequest(GET, routes.AuthController.signout(Some(RedirectUrl(sentLocation)), Some(Origin("STORE_MY_NINO"))).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9514/feedback/STORE_MY_NINO")
      }
    }

    "must return unknown when no origin is present when url not supplied" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val mockWrapperService = mock[WrapperService]
      when(mockWrapperService.safeSignoutUrl(any)).thenReturn(None)

      val application =
        applicationBuilder()
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository),
            bind[WrapperService].toInstance(mockWrapperService))
          .build()

      running(application) {

        val request = FakeRequest(GET, routes.AuthController.signout(None, None).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9514/feedback/unknown")
      }
    }

    "must clear user answers and redirect to sign out when safeSignOutUrl returns none" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val mockWrapperService = mock[WrapperService]
      when(mockWrapperService.safeSignoutUrl(any)).thenReturn(None)

      val application =
        applicationBuilder()
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository),
            bind[WrapperService].toInstance(mockWrapperService))
          .build()

      running(application) {

        val request = FakeRequest(GET, routes.AuthController.signout(None, Some(Origin("STORE_MY_NINO"))).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some("http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:9514/feedback/STORE_MY_NINO")
      }
    }
  }
}
