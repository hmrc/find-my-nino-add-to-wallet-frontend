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
import play.api.libs.json.{Format, OFormat, __}

import java.time.LocalDate

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

object IndividualDetailsData {
  implicit val individualDetailsDataFormat: Format[IndividualDetailsData] =
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
}

case class IndividualDetailsDataCache(
  id: String,
  individualDetailsData: IndividualDetailsData
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

  implicit val individualDetailsDataCacheFormat: Format[IndividualDetailsDataCache] =
    ((__ \ "id").format[String]
      ~ (__ \ "individualDetails").format[IndividualDetailsData])(
      IndividualDetailsDataCache.apply,
      unlift(iddc => Some(Tuple2(iddc.id, iddc.individualDetailsData)))
    )
}
