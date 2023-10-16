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

package connectors

import util.WireMockHelper
import org.mockito.MockitoSugar._
import play.api.libs.json.{JsValue, Json}
import config.ConfigDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.{DefaultAwaitTimeout, Injecting}
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import scala.concurrent.Future

class GovUKWalletSMNConnectorSpec extends ConnectorSpec with WireMockHelper
  with MockitoSugar
  with DefaultAwaitTimeout
  with Injecting {

  val mockConfigDecorator = mock[ConfigDecorator]
  val mockHttpClient = mock[HttpClient]
  val mockHeaderCarrier = mock[HeaderCarrier]

  val connector = new GovUKWalletSMNConnector(mockConfigDecorator, mockHttpClient)

  "GovUKWalletSMNConnector"  must {
    "create a GovUKPass" ignore {
      // Define test data
      val givenName = List("John", "Doe")
      val familyName = "Smith"
      val nino = "AB123456C"

      val headers = Map("Content-Type" -> Seq("application/json"))

      // Mock the dependencies
      when(mockConfigDecorator.findMyNinoServiceUrl).thenReturn("http://example.com")
      when(mockHttpClient.POST[JsValue, HttpResponse](any(), any())(any(),any(),any(),any()))
        .thenReturn(Future.successful(HttpResponse(200, Json.parse("""{"key":"value"}"""), headers)))

      when(mockHeaderCarrier.withExtraHeaders(any())).thenReturn(mockHeaderCarrier)

      // Call the method under test
      val result = connector.createGovUKPass(givenName, familyName, nino)

      // Verify the interactions and assertions
      verify(mockConfigDecorator).findMyNinoServiceUrl
      verify(mockHttpClient).POST[JsValue, HttpResponse](any(), any())
      verify(mockHeaderCarrier).withExtraHeaders(any())

      // Check the result
      whenReady(result) { response =>
        response shouldBe Some("""{"key":"value"}""")
      }
    }
  }
}
