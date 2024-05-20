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

import connectors.NPSConnector
import models.nps.CRNUpliftRequest
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{
  BadRequestException,
  ForbiddenException,
  HeaderCarrier,
  HttpException,
  InternalServerException,
  NotFoundException,
  UnprocessableEntityException
}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NPSService @Inject()(connector: NPSConnector)(implicit ec: ExecutionContext) extends Logging {

  def upliftCRN(identifier: String, crnUpliftRequest: CRNUpliftRequest)
               (implicit hc: HeaderCarrier): Future[Either[HttpException, Boolean]] = {
    for {
      httpResponse <- connector.upliftCRN(identifier, crnUpliftRequest)
    } yield httpResponse.status match {
      case NO_CONTENT => Right(true)
      case BAD_REQUEST =>
        logger.warn(s"${this.getClass.getName}-upliftCRN returned: ${httpResponse.status.toString}")
        throw new BadRequestException(httpResponse.body)
      case FORBIDDEN =>
        logger.warn(s"${this.getClass.getName}-upliftCRN returned: ${httpResponse.status.toString}")
        throw new ForbiddenException(httpResponse.body)
      case UNPROCESSABLE_ENTITY =>
        logger.warn(s"${this.getClass.getName}-upliftCRN returned: ${httpResponse.status.toString}")
        throw new UnprocessableEntityException(httpResponse.body)
      case NOT_FOUND =>
        logger.warn(s"${this.getClass.getName}-upliftCRN returned: ${httpResponse.status.toString}")
        throw new NotFoundException(httpResponse.body)
      case INTERNAL_SERVER_ERROR =>
        logger.error(s"${this.getClass.getName}-upliftCRN returned: ${httpResponse.status.toString}")
        throw new InternalServerException(httpResponse.body)
    }
  }
}