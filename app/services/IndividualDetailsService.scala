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

import com.google.inject.ImplementedBy
import connectors.IndividualDetailsConnector
import models.individualDetails._
import org.mongodb.scala.MongoException
import play.api.Logging
import play.api.http.Status.OK
import repositories.IndividualDetailsRepoTrait
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[IndividualDetailsServiceImpl])
trait IndividualDetailsService {
  def getIdDataFromCache(nino: String, sessionId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[String, IndividualDetailsDataCache]]
}


class IndividualDetailsServiceImpl @Inject()(
                                              individualDetailsRepository: IndividualDetailsRepoTrait,
                                              individualDetailsConnector: IndividualDetailsConnector
                                            )(implicit ec: ExecutionContext)
  extends IndividualDetailsService with Logging {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def insertOrReplaceIndividualDetailsDataCache(sessionId: String, individualDetails: IndividualDetails): Future[String] = {
    individualDetailsRepository.insertOrReplaceIndividualDetailsDataCache(
      getIndividualDetailsDataCache(sessionId, individualDetails)
    )
  }

  private def getIndividualDetailsDataCache(nino: String): Future[Option[IndividualDetailsDataCache]] =
    individualDetailsRepository.findIndividualDetailsDataByNino(nino) map {
      case Some(individualDetailsData) => Some(individualDetailsData)
      case _ => None
    } recover({
      case e: MongoException =>
        logger.warn(s"Failed finding Individual Details Data by NINO: $nino, ${e.getMessage}")
        None
    })

  private def getIndividualDetailsDataCache(sessionId: String, individualDetails: IndividualDetails): IndividualDetailsDataCache = {

    val iDetails = IndividualDetailsData(
      individualDetails.getFullName,
      individualDetails.getFirstForename,
      individualDetails.getLastName,
      individualDetails.getInitialsName,
      individualDetails.getNino,
      individualDetails.getAddressData
      )

    IndividualDetailsDataCache(
      sessionId,
      Some(iDetails)
    )
  }

  private def fetchIndividualDetails(nino: String)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]] = {
    individualDetailsConnector.getIndividualDetails(nino, "Y").flatMap(handleResponse(nino))
  }

  private def handleResponse(str: String)(response: HttpResponse): Future[Either[IndividualDetailsError, IndividualDetails]] = {
    response.status match {
      case OK =>
        Future.successful(Right(response.json.as[IndividualDetails]))
      case _ =>
        Future.successful(Left(ConnectorError(response.status, response.body)))
    }
  }

  private def createNewIndividualDataCache(nino: String, sessionId: String)(
    implicit ec: ExecutionContext, hc: HeaderCarrier
  ): Future[Either[String, IndividualDetailsDataCache]] = {
    fetchIndividualDetails(nino)(ec,hc).flatMap {
      case Right(individualDetails) =>
        insertOrReplaceIndividualDetailsDataCache(sessionId, individualDetails).map { ninoStr =>
          if(ninoStr.nonEmpty)
            Right(getIndividualDetailsDataCache(sessionId, individualDetails))
          else
            Left("Failed to create individual details data cache")
        }
      case Left(error) =>
        Future.successful(Left(s"Failed to fetch individual details: ${error.toString}"))
    }
  }

  def getIdDataFromCache(nino: String, sessionId: String)(implicit ec: ExecutionContext,
                                                          hc: HeaderCarrier): Future[Either[String, IndividualDetailsDataCache]] = {
    getIndividualDetailsDataCache(nino).flatMap {
      case Some(individualDetailsDataCache) =>
        logger.info(s"Individual details found in cache for Nino: ${nino}")
        Future.successful(Right(individualDetailsDataCache))
      case None =>
        logger.warn("Individual details data cache not found, potentially expired, attempting to fetch from external service and recreate cache.")
        createNewIndividualDataCache(nino, sessionId)
    }
  }

}
