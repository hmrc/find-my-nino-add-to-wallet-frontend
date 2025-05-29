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
import com.google.inject.ImplementedBy
import connectors.IndividualDetailsConnector
import models.individualDetails.IndividualDetailsDataCache
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[IndividualDetailsServiceImpl])
trait IndividualDetailsService {
  def getIdData(nino: String, sessionId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetailsDataCache]

  def deleteIdData(nino: String)(implicit ec: ExecutionContext): Future[Boolean]
}

class IndividualDetailsServiceImpl @Inject() (
  connector: IndividualDetailsConnector
) extends IndividualDetailsService
    with Logging {

  override def getIdData(nino: String, sessionId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetailsDataCache] =
    connector.getIndividualDetails(nino, sessionId)

  override def deleteIdData(nino: String)(implicit ec: ExecutionContext): Future[Boolean] =
    connector.deleteIndividualDetails(nino)
}
