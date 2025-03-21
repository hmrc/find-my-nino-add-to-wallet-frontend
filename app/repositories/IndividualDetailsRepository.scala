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

package repositories

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import models.individualDetails.IndividualDetailsDataCache
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model._
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import com.mongodb.client.result.DeleteResult

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualDetailsRepository @Inject()(mongoComponent: MongoComponent,
                                            appConfig: FrontendAppConfig
                                            )(implicit ec: ExecutionContext) extends PlayMongoRepository[IndividualDetailsDataCache](
  collectionName = "individual-details",
  mongoComponent = mongoComponent,
  domainFormat = IndividualDetailsDataCache.individualDetailsDataCacheFormat,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("id"),
      IndexOptions().name("idIdx")
    ),
    IndexModel(
      Indexes.ascending("individualDetails.nino"),
      IndexOptions().name("ninoIdx")
    ),
    IndexModel(
      Indexes.ascending("lastUpdated"),
      IndexOptions()
        .name("lastUpdatedIdx")
        .expireAfter(appConfig.individualDetailsCacheTtl, TimeUnit.SECONDS)
    )
  ),
  replaceIndexes = true
) with Logging with IndividualDetailsRepoTrait {
  def insertOrReplaceIndividualDetailsDataCache(individualDetailsData: IndividualDetailsDataCache)
                                               (implicit ec: ExecutionContext): Future[String] = {
    logger.info(s"insert or update one in $collectionName table")

    val filter = Filters.equal("individualDetails.nino", individualDetailsData.individualDetailsData.nino)
    val options = ReplaceOptions().upsert(true)
    collection.replaceOne(filter, individualDetailsData, options)
      .toFuture()
      .map(_ => individualDetailsData.individualDetailsData.nino) recover {
      case e: MongoWriteException if e.getCode == 11000 =>
        logger.warn(s"Error replacing or updating into $collectionName table")
        ""
    }
  }

  def findIndividualDetailsDataByNino(nino: String)
                               (implicit ec: ExecutionContext): Future[Option[IndividualDetailsDataCache]] = {
    //logger.info(s"find one in $collectionName table")
    val filter = Filters.regex("individualDetails.nino", nino.take(8).r)
    collection.find(filter)
      .toFuture()
      .map(_.headOption)
  } recoverWith {
    case e: Throwable =>
      logger.warn(s"Failed finding Individual Details Data by Nino: $nino")
      Future.failed(e)
  }

  def deleteIndividualDetailsDataByNino(nino: String)
                                       (implicit ec: ExecutionContext): Future[DeleteResult] = {
    val filter = Filters.regex("individualDetails.nino", nino.take(8).r)
    collection.deleteOne(filter)
      .toFuture()
  } recoverWith {
    case e: Exception =>
      logger.warn(s"Failed deleting Individual Details Data by Nino")
      Future.failed(e)
  }

}
