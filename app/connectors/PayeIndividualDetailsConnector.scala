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
import config.FrontendAppConfig
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

sealed trait IndividualDetailsResponse

case class IndividualDetailsSuccessResponse(str: String) extends IndividualDetailsResponse

case object IndividualDetailsNotFoundResponse extends IndividualDetailsResponse

case object IndividualDetailsHiddenResponse extends IndividualDetailsResponse

case class IndividualDetailsUnexpectedResponse(r: HttpResponse) extends IndividualDetailsResponse

case class IndividualDetailsErrorResponse(cause: Throwable) extends IndividualDetailsResponse

class PayeIndividualDetailsConnector @Inject()(
                                                val httpClientV2: HttpClientV2,
                                                val metrics: Metrics,
                                                frontendAppConfig: FrontendAppConfig
                                              )(implicit val ec: ExecutionContext) extends Logging {

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def individualDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[IndividualDetailsResponse] = {
    val url                                     = url"${frontendAppConfig.api1303ServiceUrl}/pay-as-you-earn/02.00.00/individuals/$nino"
    httpClientV2
      .get(url)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(res)                                      => Future.successful(IndividualDetailsSuccessResponse(res.body))
        case Left(UpstreamErrorResponse.WithStatusCode(404)) =>
          logger.warn("Unable to find individual details record")
          Future.successful(IndividualDetailsNotFoundResponse)
        case Left(err)                                       => Future.failed(new RuntimeException(s"Call to $url failed with upstream error: ${err.message}"))
      }
      .recover {
        case NonFatal(ex) =>
          logger.warn("Error getting individual details from API 1303", ex)
          IndividualDetailsErrorResponse(ex)
      }
  }
}


