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
  def getIndividualDetails(nino: String, resolveMerge: String)(implicit
                                                               hc: HeaderCarrier,
                                                               ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetails]
}

@Singleton
class DefaultIndividualDetailsConnector @Inject()(
                                                   httpClient: HttpClientV2,
                                                   appConfig: FrontendAppConfig,
                                                   httpClientResponse: HttpClientResponse
                                                 )
  extends IndividualDetailsConnector
    with Logging {

  override def getIndividualDetails(nino: String, resolveMerge: String)(implicit
                                                                        hc: HeaderCarrier,
                                                                        ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetails] = {
    val url =
      s"${appConfig.individualDetailsServiceUrl}/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/$resolveMerge"

    httpClientResponse.read(
      httpClient
        .get(url"$url")
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
    ).map(_.json.as[IndividualDetails])
  }
}

@Singleton
class CachingIndividualDetailsConnector @Inject()(
                                                   @javax.inject.Named("default") underlying: IndividualDetailsConnector,
                                                   repo: IndividualDetailsRepoTrait
                                                 )(implicit ec: ExecutionContext)
  extends IndividualDetailsConnector with Logging {

  override def getIndividualDetails(nino: String, resolveMerge: String)(implicit
                                                                        hc: HeaderCarrier,
                                                                        ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetails] =
    underlying.getIndividualDetails(nino, resolveMerge)

  def getIndividualDetailsWithCache(nino: String, sessionId: String)(implicit
                                                                     hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetailsDataCache] = EitherT {
    repo.findIndividualDetailsDataByNino(nino).flatMap {
      case Some(cache) =>
        logger.info(s"Cache hit for IndividualDetails: $nino")
        Future.successful(Right(cache))

      case None =>
        logger.info(s"Cache miss for IndividualDetails: $nino, fetching from service.")
        getIndividualDetails(nino, "Y").semiflatMap { details =>
          val cache = IndividualDetailsDataCache(
            sessionId,
            IndividualDetailsData(
              details.getFullName,
              details.preferredName.firstForename,
              details.preferredName.surname,
              details.getInitialsName,
              details.dateOfBirth,
              details.getNino,
              details.getAddressData,
              details.crnIndicator.asString
            )
          )
          repo.insertOrReplaceIndividualDetailsDataCache(cache).map(_ => cache)
        }.value
    } recover {
      case ex =>
        logger.warn(s"Cache lookup failed for nino=$nino: ${ex.getMessage}")
        Left(UpstreamErrorResponse(s"Cache error: ${ex.getMessage}", 500))
    }
  }

  def deleteIndividualDetailsCache(nino: String): Future[Boolean] =
    repo.deleteIndividualDetailsDataByNino(nino).map(_.wasAcknowledged())
}