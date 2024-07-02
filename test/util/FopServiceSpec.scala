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
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.FopService
import util.Fixtures.{fakeIndividualDetails, fakeIndividualDetailsDataCache}

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext
import scala.xml.Utility.trim
import scala.xml.{Elem, XML}

class FopServiceSpec extends SpecBase with MockitoSugar with CDFixtures {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val messages: Messages = messagesApi.preferred(request)
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val expectedXML: Elem = <root>
    <initials-name>FML</initials-name>
    <full-name>Dr Firstname Middlename Lastname Phd.</full-name>
    <address>
      <address-line>123 Fake Street</address-line>
      <address-line>Apt 4B</address-line>
      <address-line>Faketown</address-line>
      <address-line>Fakeshire</address-line>
      <address-line>Fakecountry</address-line>
    </address>
    <postcode>AA1 1AA</postcode>
    <nino>{fakeIndividualDetails.getNino.grouped(2).mkString(" ")}</nino>
    <date>01/23</date>
  </root>

  class Setup {
    val fopService: FopService = app.injector.instanceOf[FopService]
  }

  "XmlFoToPDF getXMLSource" - {
    "return expected XML when passed in valid person details and a date" in new Setup {
      val bytes: Array[Byte] = fopService.getXMLSource(fakeIndividualDetailsDataCache, "01/23")
      val result = new String(bytes, StandardCharsets.UTF_8)
      val xmlResult: Elem = XML.loadString(result)
      trim(xmlResult).must(equal(trim(expectedXML)))
    }

    "must use address" in new Setup {
      val bytes: Array[Byte] = fopService.getXMLSource(fakeIndividualDetailsDataCache, "01/23")
      val result = new String(bytes, StandardCharsets.UTF_8)
      result.contains("123 Fake Street") mustBe true
    }
  }

  "XmlFoToPDF createPDF" - {
    "must have correct contents for the PDF" in new Setup {
      fopService.createPDF(fakeIndividualDetailsDataCache, "01/23", messages).map( result =>
          result.length must be > 0
      )
    }
  }
}
