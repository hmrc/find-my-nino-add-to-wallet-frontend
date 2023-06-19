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

import com.kenshoo.play.metrics.Metrics
import config.ConfigDecorator
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.test.{DefaultAwaitTimeout, Injecting}
import services.http.SimpleHttp
import uk.gov.hmrc.domain.{Generator, Nino}
import util.WireMockHelper

import scala.util.Random

class PayeIndividualDetailsConnectorSpec
  extends ConnectorSpec
  with WireMockHelper
  with MockitoSugar
  with DefaultAwaitTimeout
  with Injecting {

  override implicit lazy val app: Application = app(
    Map("microservice.services.identity-verification-frontend.port" -> server.port())
  )

  trait SpecSetup {
    def url: String

    lazy val connector = {
      val httpClient = app.injector.instanceOf[SimpleHttp]
      val metrics = app.injector.instanceOf[Metrics]
      val configDecorator = app.injector.instanceOf[ConfigDecorator]
      new PayeIndividualDetailsConnector(httpClient, metrics, configDecorator)
    }
  }

  val nino: Nino = Nino(new Generator(new Random()).nextNino.nino)

  "PayeIndividualDetailsConnector" must {

    trait LocalSetup extends SpecSetup {
      val metricId = "individual-details"
      def url: String = s"/pay-as-you-earn/02.00.00/individuals/$nino"
    }

    "the getPersonalDetails method" must {

      "return 200 for a successful nino" ignore new LocalSetup {
        stubGet(url, OK, None)
        val result = connector.individualDetails(nino).futureValue
        result mustBe IndividualDetailsSuccessResponse(any())
      }

      "return 404 status when call to DES fails" in new LocalSetup {
        stubGet(url, NOT_FOUND, None)
        val result = connector.individualDetails(nino).futureValue
        result mustBe IndividualDetailsNotFoundResponse
      }

      "return 500 for a successful nino" ignore new LocalSetup {
        stubGet(url, INTERNAL_SERVER_ERROR, None)
        val result = connector.individualDetails(nino).futureValue
        result mustBe IndividualDetailsSuccessResponse
      }

    }
  }

}
