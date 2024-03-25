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

package services

import config.FrontendAppConfig
import connectors.IndividualDetailsConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IndividualDetailsServiceSpec extends PlaySpec with MockitoSugar {

  "IndividualDetailsService" should {

    "return the expected result from getIndividualDetails" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val ec: ExecutionContext = global

      val nino = "AB123456C"
      val resolveMerge = "Y"

      val mockFronendAppConfig = mock[FrontendAppConfig]
      val mockHttpClient = mock[HttpClient]
      val mockConnector = new IndividualDetailsConnector(mockHttpClient, mockFronendAppConfig)
      val service = new IndividualDetailsService(mockConnector)(ec,hc)
      val expectedResponse = HttpResponse(OK, "response body")

      when(mockFronendAppConfig.individualDetailsServiceUrl).thenReturn("someUrl")

      when(mockHttpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = await(service.getIndividualDetails(nino, resolveMerge))

      result mustBe expectedResponse
    }
  }
}