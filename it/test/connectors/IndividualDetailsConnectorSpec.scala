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

package connectors

import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IndividualDetailsConnectorSpec
  extends AnyFlatSpec
    with Matchers
    with ScalaFutures {


  "IndividualDetailsConnector" should "call the correct endpoint" in {
    val mockHttpClient = mock[HttpClientV2]
    val appConfig = mock[FrontendAppConfig]
    val connector = new IndividualDetailsConnector(mockHttpClient, appConfig)
    val requestBuilder: RequestBuilder = mock[RequestBuilder]

    when(appConfig.individualDetailsServiceUrl).thenReturn("http://localhost:8080")
    when(requestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(HttpResponse(200, "{}")))
    when(mockHttpClient.get(eqTo(url"http://localhost:8080/find-my-nino-add-to-wallet/individuals/details/NINO/QQ000003/Y"))(any()))
      .thenReturn(requestBuilder)

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val result = connector.getIndividualDetails("QQ000003B", "Y")(hc, global)

    result.futureValue.status shouldBe 200
  }


}