/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.data.EitherT
import config.FrontendAppConfig
import connectors.NPSConnector
import models.nps.CRNUpliftRequest
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NPSService @Inject() (connector: NPSConnector, frontendAppConfig: FrontendAppConfig)(implicit
  ec: ExecutionContext
) extends Logging {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val className: String                       = s"${this.getClass.getName}:upliftCRN"
  private val alreadyAnAdultErrorCode: String = frontendAppConfig.crnUpliftAPIAlreadyAdultErrorCode
  def upliftCRN(identifier: String, crnUpliftRequest: CRNUpliftRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Boolean] =
    connector.upliftCRN(identifier, crnUpliftRequest).transform {
      case Right(httpResponse) if httpResponse.status == NO_CONTENT => Right(true)
      case Right(httpResponse)
          if httpResponse.status == UNPROCESSABLE_ENTITY && httpResponse.body.contains(alreadyAnAdultErrorCode) =>
        Right(true)
      case Right(httpResponse)                                      => Left(UpstreamErrorResponse("", httpResponse.status))
      case Left(upstreamErrorResponse)                              => Left(upstreamErrorResponse)
    }
}
