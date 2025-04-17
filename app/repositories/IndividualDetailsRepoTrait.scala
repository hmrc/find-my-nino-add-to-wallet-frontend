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

import models.individualDetails.IndividualDetailsDataCache
import com.mongodb.client.result.DeleteResult

import scala.concurrent.{ExecutionContext, Future}

trait IndividualDetailsRepoTrait {
  def insertOrReplaceIndividualDetailsDataCache(individualDetailsData: IndividualDetailsDataCache)(implicit
    ec: ExecutionContext
  ): Future[String]

  def findIndividualDetailsDataByNino(nino: String)(implicit
    ec: ExecutionContext
  ): Future[Option[IndividualDetailsDataCache]]

  def deleteIndividualDetailsDataByNino(nino: String)(implicit ec: ExecutionContext): Future[DeleteResult]

}
