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

package connectors

import cats.data.EitherT
import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.Logging
import play.api.http.Status.{BAD_GATEWAY, NOT_FOUND, TOO_MANY_REQUESTS, UNPROCESSABLE_ENTITY}
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class HttpClientResponse @Inject() (frontendAppConfig: FrontendAppConfig)(implicit ec: ExecutionContext)
    extends Logging {

  private val alreadyAnAdultErrorCode: String = frontendAppConfig.crnUpliftAPIAlreadyAdultErrorCode

  def read(
    response: Future[Either[UpstreamErrorResponse, HttpResponse]]
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    EitherT(response.map {
      case Right(response)
          // only reachable if we use the corresponding reads - readEitherOfWithUnProcessableEntity
          if response.status == UNPROCESSABLE_ENTITY && response.body.contains(alreadyAnAdultErrorCode) =>
        logger.info("UNPROCESSABLE_ENTITY - alreadyAnAdultErrorCode")
        Right(response)
      case Right(response) if response.status == NOT_FOUND                                 =>
        // only reachable if we use the corresponding reads - readEitherOfWithNotFound
        Right(response)
      case Right(response)                                                                 =>
        Right(response)
      case Left(error) if error.statusCode >= 500 || error.statusCode == TOO_MANY_REQUESTS =>
        logger.error(error.message)
        Left(error)
      case Left(error)                                                                     =>
        logger.error(error.message, error)
        Left(error)
    } recover {
      case exception: HttpException =>
        logger.error(exception.message)
        Left(UpstreamErrorResponse(exception.message, BAD_GATEWAY, BAD_GATEWAY))
      case exception: Exception     =>
        throw exception
    })
}
