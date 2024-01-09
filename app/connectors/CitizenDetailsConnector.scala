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

import com.google.inject.{Inject, Singleton}
import com.kenshoo.play.metrics.Metrics
import config.FrontendAppConfig
import models._
import play.api.Logging
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

sealed trait PersonDetailsResponse
case class PersonDetailsSuccessResponse(personDetails: PersonDetails) extends PersonDetailsResponse
case object PersonDetailsNotFoundResponse extends PersonDetailsResponse
case object PersonDetailsHiddenResponse extends PersonDetailsResponse
case class PersonDetailsUnexpectedResponse(r: HttpResponse) extends PersonDetailsResponse
case class PersonDetailsErrorResponse(cause: Throwable) extends PersonDetailsResponse

@Singleton
class CitizenDetailsConnector @Inject() (
  val httpClientV2: HttpClientV2,
  val metrics: Metrics,
  config: FrontendAppConfig
) extends  Logging {

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def personDetails(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PersonDetailsResponse] = {
    val url                                     = url"${config.citizenDetailsServiceUrl}/citizen-details/$nino/designatory-details"
    httpClientV2
      .get(url)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(res)                                      =>
          Future.successful(PersonDetailsSuccessResponse(res.json.as[PersonDetails]))
        case Left(UpstreamErrorResponse.WithStatusCode(423)) =>
          logger.warn("Personal details record in citizen-details was hidden")
          Future.successful(PersonDetailsHiddenResponse)
        case Left(UpstreamErrorResponse.WithStatusCode(404)) =>
          logger.warn("Unable to find personal details record in citizen-details")
          Future.successful(PersonDetailsNotFoundResponse)
        case Left(resp@UpstreamErrorResponse.WithStatusCode(500)) =>
          logger.warn(
            s"Unexpected 500 response getting personal details record from citizen-details"
          )
          Future.successful(PersonDetailsUnexpectedResponse(HttpResponse(resp.statusCode, resp.message)))
        case Left(err)                                       => Future.failed(new RuntimeException(s"Call to $url failed with upstream error: ${err.message}"))
      }
      .recover {
        case NonFatal(ex) =>
          logger.warn("Error getting personal details record from citizen-details", ex)
          PersonDetailsErrorResponse(ex)
      }
    }

}
