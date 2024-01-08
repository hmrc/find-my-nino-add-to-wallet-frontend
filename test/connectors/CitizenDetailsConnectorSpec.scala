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

import com.kenshoo.play.metrics.Metrics
import config.FrontendAppConfig
import models._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import services.http.SimpleHttp
import uk.gov.hmrc.domain.{Generator, Nino}
import util.Fixtures.buildPersonDetails
import util.WireMockHelper

import java.time.LocalDate
import scala.util.Random

class CitizenDetailsConnectorSpec
  extends ConnectorSpec
    with WireMockHelper
    with MockitoSugar
    with DefaultAwaitTimeout
    with Injecting {

  override implicit lazy val app: Application = app(
    Map("microservice.services.citizen-details-service.port" -> server.port())
  )

  val nino: Nino = Nino(new Generator(new Random()).nextNino.nino)

  trait SpecSetup {

    def url: String

    val personDetails: PersonDetails = buildPersonDetails

    val address: Address = Address(
      line1 = Some("1 Fake Street"),
      line2 = Some("Fake Town"),
      line3 = Some("Fake City"),
      line4 = Some("Fake Region"),
      line5 = None,
      postcode = None,
      country = Some("AA1 1AA"),
      startDate = Some(LocalDate.of(2015, 3, 15)),
      endDate = None,
      `type` = Some("Residential"),
      isRls = false
    )

    lazy val connector = {
      val httpClient = app.injector.instanceOf[SimpleHttp]
      val metrics = app.injector.instanceOf[Metrics]
      val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
      new CitizenDetailsConnector(httpClient, metrics, frontendAppConfig)
    }
  }

  "Calling personDetails" must {

    trait LocalSetup extends SpecSetup {
      val metricId = "get-person-details"

      def url: String = s"/citizen-details/$nino/designatory-details"
    }

    "return OK when called with an existing nino" in new LocalSetup {
      stubGet(url, OK, Some(Json.toJson(personDetails).toString()))
      val result = connector.personDetails(nino.nino).futureValue.leftSideValue
      result.asInstanceOf[PersonDetailsSuccessResponse].personDetails mustBe personDetails
    }

    "return NOT_FOUND when called with an unknown nino" in new LocalSetup {
      stubGet(url, NOT_FOUND, None)
      val result = connector.personDetails(nino.nino).futureValue
      result mustBe PersonDetailsNotFoundResponse

    }

  }
}