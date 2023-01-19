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

import com.google.inject.{Inject, Singleton}
import com.kenshoo.play.metrics.Metrics
import metrics._
import models._
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import services.http.SimpleHttp
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

sealed trait PersonDetailsResponse
case class PersonDetailsSuccessResponse(personDetails: PersonDetails) extends PersonDetailsResponse
case object PersonDetailsNotFoundResponse extends PersonDetailsResponse
case object PersonDetailsHiddenResponse extends PersonDetailsResponse
case class PersonDetailsUnexpectedResponse(r: HttpResponse) extends PersonDetailsResponse
case class PersonDetailsErrorResponse(cause: Exception) extends PersonDetailsResponse



@Singleton
class CitizenDetailsConnector @Inject() (
  val simpleHttp: SimpleHttp,
  val metrics: Metrics,
  servicesConfig: ServicesConfig
) extends HasMetrics
    with Logging {

  lazy val citizenDetailsUrl = servicesConfig.baseUrl("citizen-details")

  def personDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetailsResponse] =
    withMetricsTimer("get-person-details") { timer =>
      simpleHttp.get[PersonDetailsResponse](s"$citizenDetailsUrl/citizen-details/$nino/designatory-details")(
        onComplete = {
          case response if response.status >= 200 && response.status < 300 =>
            timer.completeTimerAndIncrementSuccessCounter()
            PersonDetailsSuccessResponse(response.json.as[PersonDetails])

          case response if response.status == LOCKED =>
            timer.completeTimerAndIncrementFailedCounter()
            logger.warn("Personal details record in citizen-details was hidden")
            PersonDetailsHiddenResponse

          case response if response.status == NOT_FOUND =>
            timer.completeTimerAndIncrementFailedCounter()
            logger.warn("Unable to find personal details record in citizen-details")
            PersonDetailsNotFoundResponse

          case response =>
            timer.completeTimerAndIncrementFailedCounter()
            if (response.status >= INTERNAL_SERVER_ERROR) {
              logger.warn(
                s"Unexpected ${response.status} response getting personal details record from citizen-details"
              )
            }
            PersonDetailsUnexpectedResponse(response)
        },
        onError = { e =>
          timer.completeTimerAndIncrementFailedCounter()
          logger.warn("Error getting personal details record from citizen-details", e)
          PersonDetailsErrorResponse(e)
        }
      )
    }


}
