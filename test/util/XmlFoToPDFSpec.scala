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

package util

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest

class XmlFoToPDFSpec extends SpecBase with MockitoSugar with CDFixtures {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val request = FakeRequest()
  val messages = messagesApi.preferred(request)
  val pd = buildPersonDetails

  class Setup {
    val xmlFoToPDF: XmlFoToPDF = new XmlFoToPDF {
      override val resourceStreamResolver: BaseResourceStreamResolver = application.injector.instanceOf[BaseResourceStreamResolver]
      override val stylesheetResourceStreamResolver: StylesheetResourceStreamResolver = application.injector.instanceOf[StylesheetResourceStreamResolver]
      override val fopURIResolver: FopURIResolver = application.injector.instanceOf[FopURIResolver]
    }
  }
  "XmlFoToPDF getXMLSource" - {
    "return correct XML when passed in valid person details and a date" in new Setup {
      val result: Array[Byte] = xmlFoToPDF.getXMLSource(pd, "01/23")
      result.length > 0 mustBe true
    }
  }
  "XmlFoToPDF createPDF" - {
    "must have correct contents for the PDF" in new Setup {
      val result: Array[Byte] = xmlFoToPDF.createPDF(pd, "01/23", messages)
      result.length must be > 0
    }
  }

}
