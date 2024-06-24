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

package util

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class XSLScalaBridgeSpec extends SpecBase with MockitoSugar with I18nSupport {

  implicit val messagesApi: MessagesApi = application.injector.instanceOf[MessagesApi]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val messages: Messages = messagesApi.preferred(request)
  implicit val lang: Lang = Lang.defaultLang

  "XSLScalaBridge getMessagesText" - {
    "must return the correct string" in {

      val result = XSLScalaBridge(messages).getMessagesText("label.your_national_insurance_number_letter")

      result mustBe messagesApi("label.your_national_insurance_number_letter")
    }
  }

  "XSLScalaBridge getMessagesTextWithParameter" - {
    "return the correct string" in {

      val date = "01/23"

      val result = XSLScalaBridge(messages).getMessagesTextWithParameter("label.hmrc_date", date)

      result mustBe messagesApi("label.hmrc_date", date)
    }
  }
}

