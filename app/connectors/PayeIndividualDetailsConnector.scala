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
import play.api.Logging
import play.api.http.Status._
import services.http.SimpleHttp
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

sealed trait IndividualDetailsResponse

case class IndividualDetailsSuccessResponse(str: String) extends IndividualDetailsResponse

case object IndividualDetailsNotFoundResponse extends IndividualDetailsResponse

case object IndividualDetailsHiddenResponse extends IndividualDetailsResponse

case class IndividualDetailsUnexpectedResponse(r: HttpResponse) extends IndividualDetailsResponse

case class IndividualDetailsErrorResponse(cause: Exception) extends IndividualDetailsResponse

class PayeIndividualDetailsConnector @Inject()(
                                                val simpleHttp: SimpleHttp,
                                                val metrics: Metrics,
                                                config: ConfigDecorator
                                              ) extends Logging {

  def individualDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[IndividualDetailsResponse] = {
    simpleHttp.get[IndividualDetailsResponse](s"${config.api1303ServiceUrl}/pay-as-you-earn/02.00.00/individuals/$nino")(
      onComplete = {
        case response if response.status >= 200 && response.status < 300 =>
          IndividualDetailsSuccessResponse(response.body)
        case response if response.status == NOT_FOUND =>
          logger.warn("Unable to find indvidual details record")
          IndividualDetailsNotFoundResponse
        case response =>
          if (response.status >= INTERNAL_SERVER_ERROR) {
            logger.warn(
              s"Unexpected ${response.status} indvidual details"
            )
          }
          IndividualDetailsUnexpectedResponse(response)
      },
      onError = { e =>
        logger.warn("Error getting individual details from API 1303", e)
        IndividualDetailsErrorResponse(e)
      }
    )
  }

}


