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

package models.individualDetails

import scala.util.control.NoStackTrace

sealed abstract class IndividualDetailsError(message: String) extends Throwable {
  val errorMessage: String = message
}

final case class ConnectorError(statusCode: Int, message: String) extends IndividualDetailsError(message)

final case class InvalidIdentifier(identifier: IndividualDetailsIdentifier)
    extends IndividualDetailsError(s"Invalid identifier: $identifier")

case object CacheNotFound extends IndividualDetailsError("cache not found")


case object LockError extends IndividualDetailsError("Could not acquire lock")


final case class DataLockedException(sdesCorrelationId: String)
    extends IndividualDetailsError(s"Item with sdesCorrelationId $sdesCorrelationId was locked")

final case class SdesResponseException(status: Int, body: String)
    extends IndividualDetailsError(s"Unexpected response from SDES, status: $status, body: $body")
    with NoStackTrace
