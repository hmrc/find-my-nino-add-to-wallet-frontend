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
import config.{ConfigDecorator, FrontendAppConfig}
import controllers.ApplicationController
import org.mockito.MockitoSugar.when
import play.api.Application
import play.api.http.Status.SEE_OTHER
import play.api.mvc.MessagesControllerComponents
import play.api.test.{FakeRequest, Helpers}
import services.IdentityVerificationFrontendService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpVerbs.GET
import views.html.identity.{CannotConfirmIdentityView, FailedIvIncompleteView, LockedOutView, SuccessView, TechnicalIssuesView, TimeOutView}

import scala.concurrent.ExecutionContext
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}



class FMNAuthTest extends SpecBase {

  // Mock configuration values
  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockIdentityVerificationFrontendService: IdentityVerificationFrontendService = mock[IdentityVerificationFrontendService]
  implicit lazy val ec = app.injector.instanceOf[ExecutionContext]

  when(mockConfig.identityVerificationUpliftUrl).thenReturn("/save-your-national-insurance-number")
  when(mockConfig.origin).thenReturn("http://example.com")
  when(mockConfig.saveYourNationalNumberFrontendHost).thenReturn("http://example.com")

  // Create a test instance of the controller
  def controller: ApplicationController =
    new ApplicationController(
      mockIdentityVerificationFrontendService,
      injected[AuthConnector],
      injected[SuccessView],
      injected[CannotConfirmIdentityView],
      injected[FailedIvIncompleteView],
      injected[LockedOutView],
      injected[TimeOutView],
      injected[TechnicalIssuesView]
    )(config, mock[ConfigDecorator], env, ec, injected[MessagesControllerComponents], mock[FrontendAppConfig])


  "Methods tests" - {



    "return a Redirect with the correct parameters" in {
      val fakeRequest = FakeRequest(GET, "/path/to/resource")
      val result = controller.uplift(None)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/save-your-national-insurance-number")
      /*val params = queryString(redirectLocation(result).get)
      params("origin") mustBe Seq("http://example.com")
      params("confidenceLevel") mustBe Seq("L200")
      params("completionURL") mustBe Seq("http://example.com/path/to/success")
      params("failureURL") mustBe Seq("http://example.com/path/to/failure")*/
    }
  }



}
