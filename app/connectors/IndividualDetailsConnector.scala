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
import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import models.individualDetails.*
import play.api.Logging
import repositories.IndividualDetailsRepoTrait
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

trait IndividualDetailsConnector {
  def getIndividualDetails(nino: String, sessionId: String)(implicit
                                                            hc: HeaderCarrier,
                                                            ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetailsDataCache]

  def deleteIndividualDetails(nino: String)(implicit
                                            ec: ExecutionContext
  ): Future[Boolean]
}

@Singleton
class DefaultIndividualDetailsConnector @Inject() (
                                                    httpClient: HttpClientV2,
                                                    appConfig: FrontendAppConfig,
                                                    httpClientResponse: HttpClientResponse
                                                  ) extends IndividualDetailsConnector
  with Logging {

  override def getIndividualDetails(nino: String, sessionId: String)(implicit
                                                                     hc: HeaderCarrier,
                                                                     ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetailsDataCache] = {
    val url = s"${appConfig.individualDetailsServiceUrl}/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"

    httpClientResponse.read(
      httpClient
        .get(url"$url")
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
    ).map { response =>
      val individualDetails = response.json.as[IndividualDetails]
      IndividualDetailsDataCache(
        sessionId,
        IndividualDetailsData(
          individualDetails.getFullName,
          individualDetails.preferredName.firstForename,
          individualDetails.preferredName.surname,
          individualDetails.getInitialsName,
          individualDetails.dateOfBirth,
          individualDetails.getNino,
          individualDetails.getAddressData,
          individualDetails.crnIndicator.asString
        )
      )
    }
  }

  override def deleteIndividualDetails(nino: String)(implicit ec: ExecutionContext): Future[Boolean] =
    Future.successful(false)
}

@Singleton
class CachingIndividualDetailsConnector @Inject() (
                                                    @javax.inject.Named("default") underlying: IndividualDetailsConnector,
                                                    repo: IndividualDetailsRepoTrait
                                                  )
  extends IndividualDetailsConnector
    with Logging {

  override def getIndividualDetails(nino: String, sessionId: String)(implicit
                                                                     hc: HeaderCarrier,
                                                                     ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetailsDataCache] = {

    EitherT {
      repo.findIndividualDetailsDataByNino(nino).flatMap {
        case Some(cache) =>
          logger.info(s"[Cache Hit] Individual details for NINO: $nino")
          Future.successful(Right(cache))

        case None =>
          logger.info(s"[Cache Miss] Fetching individual details for NINO: $nino")
          underlying.getIndividualDetails(nino, sessionId).semiflatMap { fetched =>
            repo.insertOrReplaceIndividualDetailsDataCache(fetched).map(_ => fetched)
          }.value
      }.recover { case ex =>
        logger.warn(s"[Cache Error] Failed to lookup individual details for NINO: $nino - ${ex.getMessage}")
        Left(UpstreamErrorResponse(s"Cache error: ${ex.getMessage}", 500))
      }
    }
  }

  override def deleteIndividualDetails(nino: String)(implicit ec: ExecutionContext): Future[Boolean] =
    repo.deleteIndividualDetailsDataByNino(nino).map(_.wasAcknowledged())
}