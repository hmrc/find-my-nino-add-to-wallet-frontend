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

import com.mongodb.client.result.DeleteResult
import config.FrontendAppConfig
import models.encryption.id.EncryptedIndividualDetailsDataCache
import models.individualDetails.IndividualDetailsDataCache
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.await
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import util.Fixtures.fakeIndividualDetailsData

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class EncryptedIndividualDetailsRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EncryptedIndividualDetailsDataCache]
    with ScalaFutures
    with OptionValues
    with IntegrationPatience
    with MockitoSugar
    with DefaultAwaitTimeout {

  private val mockAppConfig = mock[FrontendAppConfig]

  protected val repository: EncryptedIndividualDetailsRepository = new EncryptedIndividualDetailsRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig
  )

  def createFakeIdCache: IndividualDetailsDataCache = IndividualDetailsDataCache(
    id = "id",
    individualDetailsData = fakeIndividualDetailsData,
    lastUpdated = Instant.EPOCH
  )

  when(mockAppConfig.cacheTtl) thenReturn 1L
  when(mockAppConfig.encryptionKey) thenReturn "z4rWoRLf7a1OHTXLutSDJjhrUzZTBE3b"

  "EncryptedIndividualDetailsRepository" - {

    "insertOrReplaceIndividualDetailsData" - {

      "must insert or replace the IndividualDetailsData" in {

        val individualDetailsDataCache: IndividualDetailsDataCache = IndividualDetailsDataCache(
          id = "id",
          individualDetailsData = fakeIndividualDetailsData,
          lastUpdated = Instant.EPOCH
        )
        val result                                                 = repository.insertOrReplaceIndividualDetailsDataCache(individualDetailsDataCache).futureValue
        result mustBe "AB123456C"
      }
    }

  }
  "findIndividualDetailsDataByNino" - {

    val individualDetailsDataCache: IndividualDetailsDataCache = IndividualDetailsDataCache(
      id = "id",
      individualDetailsData = fakeIndividualDetailsData,
      lastUpdated = Instant.EPOCH
    )

    "must return the IndividualDetailsData when it exists" in {

      val nino = "AB123456"

      val result1 = repository.insertOrReplaceIndividualDetailsDataCache(individualDetailsDataCache).futureValue
      result1 mustBe "AB123456C"

      // we search with NINO plus suffix
      val result = repository.findIndividualDetailsDataByNino(nino + "C").futureValue
      result.value.copy(lastUpdated = Instant.EPOCH) mustEqual individualDetailsDataCache.copy(lastUpdated =
        Instant.EPOCH
      )
    }

    "must return None when the IndividualDetailsData does not exist" in {
      val nino   = "ZZ999999Z"
      val result = repository.findIndividualDetailsDataByNino(nino).futureValue
      result mustBe None
    }
  }
  "deleteIndividualDetailsData" - {

    "must delete the cache and return a DeleteResult" in {
      val nino                       = "AB123456C"
      val individualDetailsDataCache = createFakeIdCache
      repository.insertOrReplaceIndividualDetailsDataCache(individualDetailsDataCache).futureValue mustBe nino
      await(repository.deleteIndividualDetailsDataByNino(nino)) mustBe DeleteResult.acknowledged(1)
      await(repository.findIndividualDetailsDataByNino(nino)) mustBe None
    }
  }
}
