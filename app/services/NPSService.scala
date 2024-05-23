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
import connectors.NPSConnector
import models.nps.CRNUpliftRequest
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NPSService @Inject()(connector: NPSConnector, frontendAppConfig: FrontendAppConfig)
                          (implicit ec: ExecutionContext) extends Logging {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val className: String                       = s"${this.getClass.getName}:upliftCRN"
  private val alreadyAnAdultErrorCode: String = frontendAppConfig.crnUpliftAPIAlreadyAdultErrorCode
  private val genericErrorResponseBody: String = "Something went wrong"

  // UNPROCESSABLE_ENTITY and BAD_REQUEST are obfuscated in Kibana as a matter of course as
  // they could contain information about; or from; the request including pii. The CRN uplift API
  // call is audited in the back end under `find-my-nino-add-to-wallet-CRNUplift`
  def upliftCRN(identifier: String, crnUpliftRequest: CRNUpliftRequest)
               (implicit hc: HeaderCarrier): Future[Either[HttpException, Int]] = {
    for {
      httpResponse <- connector.upliftCRN(identifier, crnUpliftRequest)
    } yield httpResponse.status match {
      case NO_CONTENT =>
        Right(NO_CONTENT)
      case UNPROCESSABLE_ENTITY if httpResponse.body.contains(alreadyAnAdultErrorCode) =>
        Right(NO_CONTENT)
      case UNPROCESSABLE_ENTITY =>
        logger.warn(s"$className returned: ${httpResponse.status}")
        Left(new HttpException(genericErrorResponseBody, UNPROCESSABLE_ENTITY))
      case BAD_REQUEST =>
        logger.warn(s"$className returned: ${httpResponse.status}")
        Left(new HttpException(genericErrorResponseBody, BAD_REQUEST))
      case FORBIDDEN =>
        logger.warn(s"$className returned: ${httpResponse.status}")
        Left(new HttpException(httpResponse.body, FORBIDDEN))
      case NOT_FOUND =>
        logger.warn(s"$className returned: ${httpResponse.status}")
        Left(new HttpException(httpResponse.body, NOT_FOUND))
      case INTERNAL_SERVER_ERROR =>
        logger.error(s"$className returned: ${httpResponse.status}")
        Left(new HttpException(httpResponse.body, INTERNAL_SERVER_ERROR))
    }
  }
}