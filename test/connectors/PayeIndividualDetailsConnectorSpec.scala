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

import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics
import config.ConfigDecorator
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.test.Helpers._
import play.api.test.Helpers.await
import play.api.test.{DefaultAwaitTimeout, Injecting}
import services.http.SimpleHttp
import uk.gov.hmrc.domain.{Generator, Nino}
import util.{CDFixtures, WireMockHelper}

import scala.util.Random

class PayeIndividualDetailsConnectorSpec extends ConnectorSpec
  with WireMockHelper
  with DefaultAwaitTimeout
  with Injecting {

  override implicit lazy val app: Application = app(
    Map("microservice.services.identity-verification-frontend.port" -> server.port())
  )

  val nino: Nino = Nino(new Generator(new Random()).nextNino.nino)

  val connector = new PayeIndividualDetailsConnector(mock[SimpleHttp], mock[Metrics], mock[ConfigDecorator])

  "PayeIndividualDetailsConnector" must {

    "the getPersonalDetails method" must {


      "return pay personal details for a successful nino" in {
        val result = connector.individualDetails(nino)

      }

      "return 500 status when call to DES fails" in {
        val result = connector.individualDetails(nino)


      }

    }

  }

}
