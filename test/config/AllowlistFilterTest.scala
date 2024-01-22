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

package config

import base.SpecBase
import controllers.routes
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._

class AllowlistFilterTest extends SpecBase with BeforeAndAfterEach {

  "AllowlistFilterTest" - {

    "when allow list is defined" - {

      "should allow listed ips" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("filters.allowlist.ips" -> "192.1.2.3, 192.1.2.4")
          .build()

        running(application) {

          implicit val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.PublicController.sessionTimeout.url)
              .withHeaders("True-Client-IP" -> "192.1.2.3")

          val result = route(application, request).value
          status(result) mustEqual OK
        }
      }

      "should reject unlisted ips" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("filters.allowlist.ips" -> "192.1.2.3, 192.1.2.4")
          .build()

        running(application) {

          implicit val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.PublicController.sessionTimeout.url)
              .withHeaders("True-Client-IP" -> "192.1.2.6")

          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result) mustEqual Some("https://www.gov.uk")

        }
      }
    }

    "when allow list is not defined" - {

      "should allow listed all ips" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .configure("filters.allowlist.ips" -> "")
          .build()

        running(application) {

          implicit val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, routes.PublicController.sessionTimeout.url)
              .withHeaders("True-Client-IP" -> "192.1.2.6")

          val result = route(application, request).value
          status(result) mustEqual OK
        }
      }
    }
  }
}
