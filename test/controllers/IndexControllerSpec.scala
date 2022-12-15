/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.Stubs.userLoggedInFMNUser
import util.TestData.NinoUser
import views.html.IndexView

class IndexControllerSpec extends SpecBase with MockitoSugar {

  "Index Controller" - {
     lazy val indexController = applicationWithConfig.injector.instanceOf[IndexController]
     lazy val view = applicationWithConfig.injector.instanceOf[IndexView]

    "must return OK and the correct view for a GET" in {

      running(applicationWithConfig){
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)
          .withSession(("sessionId", "someId"))
          .withSession(("authToken","Bearer 123"))


        val result = indexController.onPageLoad(request)
        status(result) mustBe OK
        contentAsString(result) mustEqual view()(request, messages(applicationWithConfig)).toString
      }

    }


    "must return 303 and the correct view for unauthorised access GET" in {
      val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)
      val result = indexController.onPageLoad(request)
      status(result) mustBe 303
      contentAsString(result) != view()(request, messages(applicationWithConfig)).toString
    }
  }
}

