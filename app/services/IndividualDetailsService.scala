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

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


@ImplementedBy(classOf[IndividualDetailsServiceImpl])
trait IndividualDetailsService {
  def createIndividualDetailsDataCache(sessionId: String, individualDetails: IndividualDetails): Future[String]

  def getIndividualDetails(nino: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]]

  def getIdDataFromCache(nino: String): Future[Either[String, IndividualDetailsDataCache]]
}


class IndividualDetailsServiceImpl @Inject()(
                                              individualDetailsRepository: IndividualDetailsRepoTrait,
                                              individualDetailsConnector: IndividualDetailsConnector
                                            )(implicit ec: ExecutionContext)
  extends IndividualDetailsService with Logging {

  override def createIndividualDetailsDataCache(sessionId: String, individualDetails: IndividualDetails): Future[String] = {
    individualDetailsRepository.insertOrReplaceIndividualDetailsDataCache(
      getIndividualDetailsData(sessionId, individualDetails)
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

  private def getIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): IndividualDetailsDataCache = {

    val iDetails = IndividualDetailsData(
      individualDetails.getFullName,
      individualDetails.getFirstForename,
      individualDetails.getLastName,
      individualDetails.getInitialsName,
      individualDetails.dateOfBirth.toString,
      individualDetails.getPostCode,
      individualDetails.getNino,
      individualDetails.getAddress
      )

    IndividualDetailsDataCache(
      sessionId,
      Some(iDetails)
    )
  }

  def getIndividualDetails(nino: String
                          )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]] = {
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


  def getIdDataFromCache(nino: String): Future[Either[String, IndividualDetailsDataCache]] = {
    getIndividualDetailsDataCache(nino).map {
      case Some(individualDetailsDataCache) =>
        logger.info(s"Individual details found in cache for Nino: ${nino}")
        Right(individualDetailsDataCache)
      case None =>
        logger.info("Individual details not found in cache")
        Left("Individual details not found in cache")
    }
  }

}
