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
import connectors.FandFConnector
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import util.Fixtures.buildFakeRequestWithAuth
import views.html.public.SessionTimeoutView

import scala.concurrent.ExecutionContext

class PublicControllerSpec extends SpecBase {

  val ec: ExecutionContext =
    scala.concurrent.ExecutionContext.global

  private def controller =
    new PublicController(injected[SessionTimeoutView], injected[AuthConnector], injected[FandFConnector])(
      frontendAppConfig,
      cc,
      config,
      env,
      ec
    )

  "Calling PublicController.sessionTimeout" - {

    "return 200" in {

      val r = controller.sessionTimeout(buildFakeRequestWithAuth("GET"))
      status(r) mustBe OK
    }
  }
}
