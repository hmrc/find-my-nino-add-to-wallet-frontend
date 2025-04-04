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

import models.encryption.EncryptedValueFormat._
import models.individualDetails._
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Format, Json, OFormat, __}
import uk.gov.hmrc.crypto.{EncryptedValue, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat

import java.time.{Instant, LocalDate}

case class EncryptedIndividualDetailsData(
                                           fullName: EncryptedValue,
                                           firstForename: EncryptedValue,
                                           surname: EncryptedValue,
                                           initialsName: EncryptedValue,
                                           dateOfBirth: EncryptedValue,
                                           nino: String,
                                           address: Option[EncryptedAddressData],
                                           crnIndicator: EncryptedValue
                                )

case class EncryptedIndividualDetailsDataCache(
  id: String,
  individualDetailsData: EncryptedIndividualDetailsData,
  lastUpdated: Instant = Instant.now(java.time.Clock.systemUTC())
)

final case class EncryptedAddressData(addressLine1: EncryptedAddressLine,
                                addressLine2: EncryptedAddressLine,
                                addressLine3: Option[EncryptedAddressLine],
                                addressLine4: Option[EncryptedAddressLine],
                                addressLine5: Option[EncryptedAddressLine],
                                addressPostcode: Option[EncryptedAddressPostcode],
                                addressCountry: EncryptedValue,
                                addressStartDate: EncryptedValue,
                                addressType: EncryptedAddressType)

object EncryptedAddressData {
  implicit val format: OFormat[EncryptedAddressData] = Json.format[EncryptedAddressData]
}

final case class EncryptedAddressLine(value: EncryptedValue) extends AnyVal
object EncryptedAddressLine {
  implicit val format: Format[EncryptedAddressLine] = Json.valueFormat[EncryptedAddressLine]
}
final case class EncryptedAddressPostcode(value: EncryptedValue) extends AnyVal
object EncryptedAddressPostcode {
  implicit val format: Format[EncryptedAddressPostcode] = Json.valueFormat[EncryptedAddressPostcode]
}

final case class EncryptedAddressType(value: EncryptedValue) extends AnyVal
object EncryptedAddressType {
  implicit val format: Format[EncryptedAddressType] = Json.valueFormat[EncryptedAddressType]
}



object EncryptedIndividualDetailsDataCache {

  private val encryptedIndividualDetailsDataFormat: OFormat[EncryptedIndividualDetailsData] = {
    ((__ \ "fullName").format[EncryptedValue]
      ~ (__ \ "firstForename").format[EncryptedValue]
      ~ (__ \ "surname").format[EncryptedValue]
      ~ (__ \ "initialsName").format[EncryptedValue]
      ~ (__ \ "dateOfBirth").format[EncryptedValue]
      ~ (__ \ "nino").format[String]
      ~ (__ \ "address").formatNullable[EncryptedAddressData]
      ~ (__ \ "crnIndicator").format[EncryptedValue]
      )(EncryptedIndividualDetailsData.apply, eidd => Tuple8(eidd.fullName, eidd.firstForename, eidd.surname, eidd.initialsName, eidd.dateOfBirth, eidd.nino, eidd.address, eidd.crnIndicator))
  }


  val encryptedIndividualDetailsDataCacheFormat: OFormat[EncryptedIndividualDetailsDataCache] = {
    ((__ \ "id").format[String]
      ~ (__ \ "individualDetails").format[EncryptedIndividualDetailsData](encryptedIndividualDetailsDataFormat)
      ~ (__ \ "lastUpdated").format[Instant](instantFormat)
      )(EncryptedIndividualDetailsDataCache.apply, eiddc => Tuple3(eiddc.id, eiddc.individualDetailsData, eiddc.lastUpdated))
  }

  def encryptField(fieldValue: String, key: String): EncryptedValue = {
    SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
  }

  def encrypt(individualDetailsDataCache: IndividualDetailsDataCache, key: String): EncryptedIndividualDetailsDataCache = {
    def e(fieldValue: String): EncryptedValue = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
    }
    val id = individualDetailsDataCache.individualDetailsData
    EncryptedIndividualDetailsDataCache(
      id = individualDetailsDataCache.id,
      individualDetailsData =
          EncryptedIndividualDetailsData(
            fullName = e(id.fullName),
            firstForename = e(id.firstForename),
            surname = e(id.surname),
            initialsName = e(id.initialsName),
            dateOfBirth = e(id.dateOfBirth.toString),
            nino = id.nino,
            address = id.address.map(
              addr =>
                EncryptedAddressData(
                  EncryptedAddressLine(e(addr.addressLine1.value)),
                  EncryptedAddressLine(e(addr.addressLine2.value)),
                  addr.addressLine3.map(x => EncryptedAddressLine(e(x.value))),
                  addr.addressLine4.map(x => EncryptedAddressLine(e(x.value))),
                  addr.addressLine5.map(x => EncryptedAddressLine(e(x.value))),
                  addr.addressPostcode.map(x => EncryptedAddressPostcode(e(x.value))),
                  e(addr.addressCountry),
                  e(addr.addressStartDate.toString),
                  addr.addressType match {
                    case AddressType.ResidentialAddress   => EncryptedAddressType(e("1"))
                    case AddressType.CorrespondenceAddress   => EncryptedAddressType(e("2"))
                }
            )),
            crnIndicator = e(id.crnIndicator)
          )
    )
  }

  def decrypt(encryptedIndividualDetailsDataCache: EncryptedIndividualDetailsDataCache, key: String): IndividualDetailsDataCache = {
    def d(field: EncryptedValue): String = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).decrypt(field, key)
    }

    val id = encryptedIndividualDetailsDataCache.individualDetailsData

    IndividualDetailsDataCache(
      id = encryptedIndividualDetailsDataCache.id,
          IndividualDetailsData(
            fullName = d(id.fullName),
            firstForename = d(id.firstForename),
            surname = d(id.surname),
            initialsName = d(id.initialsName),
            dateOfBirth = LocalDate.parse(d(id.dateOfBirth)),
            nino = id.nino,
            address = id.address.map(
              addr =>
                AddressData(
                  AddressLine(d(addr.addressLine1.value)),
                  AddressLine(d(addr.addressLine2.value)),
                  addr.addressLine3.map(x => AddressLine(d(x.value))),
                  addr.addressLine4.map(x => AddressLine(d(x.value))),
                  addr.addressLine5.map(x => AddressLine(d(x.value))),
                  addr.addressPostcode.map(x => AddressPostcode(d(x.value))),
                  d(addr.addressCountry),
                  LocalDate.parse(d(addr.addressStartDate)),
                  d(addr.addressType.value) match {
                    case "1" => AddressType.ResidentialAddress
                    case _   => AddressType.CorrespondenceAddress
                  }
                )),
            crnIndicator = d(id.crnIndicator)
          )
    )
  }
}