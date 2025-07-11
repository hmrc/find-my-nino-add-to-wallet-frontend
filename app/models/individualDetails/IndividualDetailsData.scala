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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat

import java.time.{Instant, LocalDate}

case class IndividualDetailsData(
  fullName: String,
  firstForename: Option[String],
  surname: Option[String],
  initialsName: String,
  dateOfBirth: LocalDate,
  nino: String,
  address: Option[AddressData],
  crnIndicator: String
)

case class IndividualDetailsDataCache(
  id: String,
  individualDetailsData: IndividualDetailsData,
  lastUpdated: Instant = Instant.now(java.time.Clock.systemUTC())
) {

  def getAddressLines: List[String] =
    individualDetailsData.address match {
      case Some(address) =>
        List(
          address.addressLine1.value,
          address.addressLine2.value,
          address.addressLine3.map(_.value).getOrElse(""),
          address.addressLine4.map(_.value).getOrElse(""),
          address.addressLine5.map(_.value).getOrElse("")
        ).filter(_.nonEmpty)
      case _             => List.empty
    }

  def getPostCode: Option[String] =
    individualDetailsData.address.flatMap(addr => addr.addressPostcode.map(postcode => postcode.value))
}

object IndividualDetailsDataCache {
  private val individualDetailsDataFormat: OFormat[IndividualDetailsData] =
    ((__ \ "fullName").format[String]
      ~ (__ \ "firstForename").formatNullable[String]
      ~ (__ \ "surname").formatNullable[String]
      ~ (__ \ "initialsName").format[String]
      ~ (__ \ "dateOfBirth").format[LocalDate]
      ~ (__ \ "nino").format[String]
      ~ (__ \ "address").formatNullable[AddressData]
      ~ (__ \ "crnIndicator").format[String])(
      IndividualDetailsData.apply,
      unlift { idd =>
        Some(
          Tuple8(
            idd.fullName,
            idd.firstForename,
            idd.surname,
            idd.initialsName,
            idd.dateOfBirth,
            idd.nino,
            idd.address,
            idd.crnIndicator
          )
        )
      }
    )

  val individualDetailsDataCacheFormat: OFormat[IndividualDetailsDataCache] =
    ((__ \ "id").format[String]
      ~ (__ \ "individualDetails").format[IndividualDetailsData](individualDetailsDataFormat)
      ~ (__ \ "lastUpdated").format[Instant](instantFormat))(
      IndividualDetailsDataCache.apply,
      unlift(iddc => Some(Tuple3(iddc.id, iddc.individualDetailsData, iddc.lastUpdated)))
    )
}
