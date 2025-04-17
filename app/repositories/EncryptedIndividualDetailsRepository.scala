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
import models.encryption.id.EncryptedIndividualDetailsDataCache
import models.encryption.id.EncryptedIndividualDetailsDataCache.{decrypt, encrypt}
import models.individualDetails.IndividualDetailsDataCache
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model._
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import org.mongodb.scala.SingleObservableFuture

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EncryptedIndividualDetailsRepository @Inject() (mongoComponent: MongoComponent, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) extends PlayMongoRepository[EncryptedIndividualDetailsDataCache](
      collectionName = "individual-details",
      mongoComponent = mongoComponent,
      domainFormat = EncryptedIndividualDetailsDataCache.encryptedIndividualDetailsDataCacheFormat,
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
    )
    with Logging
    with IndividualDetailsRepoTrait {
  def insertOrReplaceIndividualDetailsDataCache(
    individualDetailsData: IndividualDetailsDataCache
  )(implicit ec: ExecutionContext): Future[String] = {
    logger.info(s"insert or update one in $collectionName table")

    val filter  = Filters.equal("individualDetails.nino", individualDetailsData.individualDetailsData.nino)
    val options = ReplaceOptions().upsert(true)
    collection
      .replaceOne(filter, encrypt(individualDetailsData, appConfig.encryptionKey), options)
      .toFuture()
      .map(_ => individualDetailsData.individualDetailsData.nino) recover {
      case e: MongoWriteException if e.getCode == 11000 =>
        logger.warn(s"Error replacing or updating into $collectionName table")
        ""
    }
  }

  def findIndividualDetailsDataByNino(
    nino: String
  )(implicit ec: ExecutionContext): Future[Option[IndividualDetailsDataCache]] = {
    logger.info(s"find one in $collectionName table")
    val filter = Filters.equal("individualDetails.nino", nino)
    collection
      .find(filter)
      .first()
      .toFutureOption()
      .map(optEncryptedIndividualDetailsDataCache =>
        optEncryptedIndividualDetailsDataCache.map(encryptedIndividualDetailsDataCache =>
          decrypt(encryptedIndividualDetailsDataCache, appConfig.encryptionKey)
        )
      ) recoverWith { case e: Throwable =>
      logger.info(s"Failed finding Individual Details Data by Nino: $nino")
      Future.failed(e)
    }
  }

  def deleteIndividualDetailsDataByNino(
    nino: String
  )(implicit ec: ExecutionContext): Future[com.mongodb.client.result.DeleteResult] = {
    val filter = Filters.equal("individualDetails.nino", nino)
    collection
      .deleteOne(filter)
      .toFuture()
  } recoverWith { case e: Exception =>
    logger.warn(s"Failed deleting Individual Details Data by Nino")
    Future.failed(e)
  }
}
