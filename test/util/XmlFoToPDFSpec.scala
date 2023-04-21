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

import java.nio.charset.StandardCharsets

class XmlFoToPDFSpec extends SpecBase with MockitoSugar with CDFixtures {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val request = FakeRequest()
  val messages = messagesApi.preferred(request)
  val pd = buildPersonDetails
  val pdWithBothAddresses = buildPersonDetailsCorrespondenceAddress
  val pdWithoutCorrespondenceAddress = buildPersonDetailsWithoutCorrespondenceAddress
  val pdWithoutAddress = buildPersonDetailsWithoutAddress

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
    "must use correspondence address by default" in new Setup {
      val bytes: Array[Byte] = xmlFoToPDF.getXMLSource(pdWithBothAddresses, "01/23")
      val result = new String(bytes, StandardCharsets.UTF_8)
      result.contains("2 Fake Street") mustBe true
    }
    "must use correspondence address if no address" in new Setup {
      val bytes: Array[Byte] = xmlFoToPDF.getXMLSource(pdWithoutAddress, "01/23")
      val result = new String(bytes, StandardCharsets.UTF_8)
      result.contains("2 Fake Street") mustBe true
    }
    "must use address if no correspondence address" in new Setup {
      val bytes: Array[Byte] = xmlFoToPDF.getXMLSource(pdWithoutCorrespondenceAddress, "01/23")
      val result = new String(bytes, StandardCharsets.UTF_8)
      result.contains("1 Fake Street") mustBe true
    }
  }
  "XmlFoToPDF createPDF" - {
    "must have correct contents for the PDF" in new Setup {
      val result: Array[Byte] = xmlFoToPDF.createPDF(pd, "01/23", messages)
      result.length must be > 0
    }
  }

}
