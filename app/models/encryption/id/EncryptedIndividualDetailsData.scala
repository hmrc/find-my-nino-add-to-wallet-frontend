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

package models.encryption.id

import models.Address
import models.encryption.EncryptedValueFormat._
import models.individualDetails.{IndividualDetailsData, IndividualDetailsDataCache}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.crypto.{EncryptedValue, SymmetricCryptoFactory}
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

case class EncryptedIndividualDetailsData(
                                           fullName: EncryptedValue,
                                           firstForename: EncryptedValue,
                                           surname: EncryptedValue,
                                           initialsName: EncryptedValue,
                                           nino: String,
                                           address: Option[EncryptedAddressData]
                                )

case class EncryptedIndividualDetailsDataCache(
  id: String,
  individualDetailsData: Option[EncryptedIndividualDetailsData],
  lastUpdated: Instant = LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC)
)

case class EncryptedAddressData(addressLine1: EncryptedAddressLine,
                                addressLine2: EncryptedAddressLine,
                                addressLine3: Option[EncryptedAddressLine],
                                addressLine4: Option[EncryptedAddressLine],
                                addressLine5: Option[EncryptedAddressLine],
                                addressPostcode: Option[String])

case class EncryptedAddressLine(value: EncryptedValue)
case class EncryptedAddressPostcode(value: EncryptedValue)

object EncryptedIndividualDetailsDataCache {

  private val encryptedIndividualDetailsDataFormat: OFormat[EncryptedIndividualDetailsData] = {
    ((__ \ "fullName").format[EncryptedValue]
      ~ (__ \ "firstForename").format[EncryptedValue]
      ~ (__ \ "surname").format[EncryptedValue]
      ~ (__ \ "initialsName").format[EncryptedValue]
      ~ (__ \ "nino").format[String]
      ~ (__ \ "address").format[EncryptedAddressData]
      )(EncryptedIndividualDetailsData.apply, unlift(EncryptedIndividualDetailsData.unapply))
  }


  val encryptedIndividualDetailsDataCacheFormat: OFormat[EncryptedIndividualDetailsDataCache] = {
    ((__ \ "id").format[String]
      ~ (__ \ "individualDetails").formatNullable[EncryptedIndividualDetailsData](encryptedIndividualDetailsDataFormat)
      ~ (__ \ "lastUpdated").format[Instant](instantFormat)
      )(EncryptedIndividualDetailsDataCache.apply, unlift(EncryptedIndividualDetailsDataCache.unapply))
  }

  private val encryptedAddressDataFormat: OFormat[EncryptedAddressData] = {
    ((__ \ "addressLine1").format[EncryptedAddressLine]
    ~ (__ \ "addressLine2").format[EncryptedAddressLine]
    ~ (__ \ "addressLine3").formatNullable[EncryptedAddressLine]
    ~ (__ \ "addressLine4").formatNullable[EncryptedAddressLine]
    ~ (__ \ "addressLine5").formatNullable[EncryptedAddressLine]
    ~ (__ \ "postcode").formatNullable[]
      )(EncryptedAddressData.apply, unlift(EncryptedAddressData.unapply))
  }

  private val encryptedAddressLineFormat: OFormat[EncryptedAddressLine] = {
    ((__ \ "value").format[EncryptedValue]
      )(EncryptedAddressLine.apply, unlift(EncryptedAddressLine.unapply))
  }

  private val encryptedAddressPostcodeFormat: OFormat[EncryptedAddressPostcode] = {
    ((__ \ "value").format[EncryptedValue]
      )(EncryptedAddressPostcode.apply, unlift(EncryptedAddressLine.unapply))
  }


  def encryptField(fieldValue: String, key: String): EncryptedValue = {
    SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
  }

  def encrypt(individualDetailsDataCache: IndividualDetailsDataCache, key: String): EncryptedIndividualDetailsDataCache = {
    def e(fieldValue: String): EncryptedValue = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
    }

    EncryptedIndividualDetailsDataCache(
      id = individualDetailsDataCache.id,
      individualDetailsData = individualDetailsDataCache.individualDetailsData.map {
        id =>
          EncryptedIndividualDetailsData(
            fullName = e(id.fullName),
            firstForename = e(id.firstForename),
            surname = e(id.surname),
            initialsName = e(id.initialsName),
            nino = id.nino,
            address =
          )
      }
    )
  }

  def decrypt(encryptedIndividualDetailsDataCache: EncryptedIndividualDetailsDataCache, key: String): IndividualDetailsDataCache = {
    def d(field: EncryptedValue): String = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).decrypt(field, key)
    }

    IndividualDetailsDataCache(
      id = encryptedIndividualDetailsDataCache.id,
      individualDetailsData = encryptedIndividualDetailsDataCache.individualDetailsData.map {
        id =>
          IndividualDetailsData(
            fullName = d(id.fullName),
            firstForename = d(id.firstForename),
            surname = d(id.surname),
            initialsName = d(id.initialsName),
            nino = id.nino,

          )
      }
    )
  }

  implicit class IndividualDetailsDataOps(private val individualDetailsData:EncryptedIndividualDetailsDataCache) extends AnyVal {

    def getNino: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.nino
      case _        => ""
    }

    def getPostCode: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.postCode.value
      case _        => ""
    }

    def getFirstForename: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.firstForename.value
      case _        => ""
    }

    def getLastName: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.surname.value
      case _        => ""
    }

    def dateOfBirth: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.dateOfBirth.value
      case _        => ""
    }
  }
}