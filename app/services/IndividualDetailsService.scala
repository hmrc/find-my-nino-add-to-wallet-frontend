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
  def getIdDataFromCache(nino: String, sessionId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[Int, IndividualDetailsDataCache]]
  def deleteIdDataFromCache(nino: String)(implicit ec: ExecutionContext): Future[Boolean]
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
      individualDetails.getDateOfBirth,
      individualDetails.getNino,
      individualDetails.getAddressData,
      individualDetails.getCrnIndicator
      )

    IndividualDetailsDataCache(
      sessionId,
      Some(iDetails)
    )
  }

  private def fetchIndividualDetails(nino: String)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[HttpResponse, IndividualDetails]] = {
    individualDetailsConnector.getIndividualDetails(nino, "Y").flatMap(handleResponse())
  }

  private def handleResponse()(response: HttpResponse): Future[Either[HttpResponse, IndividualDetails]] = {
    response.status match {
      case OK =>
        Future.successful(Right(response.json.as[IndividualDetails]))
      case _ =>
        Future.successful(Left(response))
    }
  }

  private def createNewIndividualDataCache(nino: String, sessionId: String)(
    implicit ec: ExecutionContext, hc: HeaderCarrier
  ): Future[Either[Int, IndividualDetailsDataCache]] = {
    fetchIndividualDetails(nino)(ec,hc).flatMap {
      case Right(individualDetails) =>
        insertOrReplaceIndividualDetailsDataCache(sessionId, individualDetails).map { ninoStr =>
          if(ninoStr.nonEmpty)
            Right(getIndividualDetailsDataCache(sessionId, individualDetails))
          else
            throw new RuntimeException("Failed to create individual details data cache")
        }
      case Left(httpResponse) =>
        Future.successful(Left(httpResponse.status))
    }
  }

  def getIdDataFromCache(nino: String, sessionId: String)(implicit ec: ExecutionContext,
                                                          hc: HeaderCarrier): Future[Either[Int, IndividualDetailsDataCache]] = {
    getIndividualDetailsDataCache(nino).flatMap {
      case Some(individualDetailsDataCache) =>
        logger.info(s"Individual details found in cache for Nino: ${nino}")
        Future.successful(Right(individualDetailsDataCache))
      case None =>
        logger.warn("Individual details data cache not found, potentially expired, attempting to fetch from external service and recreate cache.")
        createNewIndividualDataCache(nino, sessionId)
    }
  }

  def deleteIdDataFromCache(nino: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    individualDetailsRepository.deleteIndividualDetailsDataByNino(nino) map {
      case r => r.wasAcknowledged()
      case _ => false
    }
  }

}
