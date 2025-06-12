/*
 * Copyright 2020 HM Revenue & Customs
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

import base.IntegrationSpecBase
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER

class LanguageSwitchControllerISpec extends IntegrationSpecBase {

  s"GET ${controllers.routes.LanguageSwitchController.switchToLanguage("en")}" must {

    "switch to English and redirect back to the referrer" in {

      val res = getRequest(
        "/language/en",
        headers = Seq(HeaderNames.REFERER -> controllers.routes.StoreMyNinoController.onPageLoad.url)
      )()

      whenReady(res) { result =>
        result must have(
          httpStatus(SEE_OTHER),
          redirectLocation(controllers.routes.StoreMyNinoController.onPageLoad.url)
        )
      }
    }
  }

  s"GET ${controllers.routes.LanguageSwitchController.switchToLanguage("cy")}" must {

    "switch to Welsh and redirect back to fallback if no referrer found on request" in {

      val res = getRequest("/language/cy")()

      whenReady(res) { result =>
        result must have(
          httpStatus(SEE_OTHER),
          redirectLocation(controllers.routes.StoreMyNinoController.onPageLoad.url)
        )
      }
    }
  }
}
