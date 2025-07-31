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
  title: Option[String],
  firstForename: Option[String],
  secondForename: Option[String], //
  surname: Option[String],
  honours: Option[String],
  dateOfBirth: LocalDate,
  nino: String,
  address: Option[AddressData],
  crnIndicator: String
) {
  def getAddressLines: List[String] =
    address match {
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

  def getPostCode: Option[String] = address.flatMap(_.addressPostcode.map(_.value))

  private def getHonours: String = honours.getOrElse("")

  private def getTitle: String = title.getOrElse("")

  def getFullName: String =
    List(
      getTitle,
      firstForename.map(_.toUpperCase()).getOrElse(""),
      secondForename.getOrElse("").toUpperCase(),
      surname.map(_.toUpperCase()).getOrElse(""),
      getHonours
    )
      .filter(_.nonEmpty)
      .mkString(" ")

  def getInitialsName: String =
    List(
      getTitle,
      firstForename.map(_.toUpperCase().take(1)).getOrElse(""),
      secondForename.getOrElse("").toUpperCase().take(1),
      surname.map(_.toUpperCase()).getOrElse(""),
      getHonours
    )
      .filter(_.nonEmpty)
      .mkString(" ")

}

object IndividualDetailsData {
  implicit val individualDetailsDataFormat: Format[IndividualDetailsData] =
    ((__ \ "title").formatNullable[String]
      ~ (__ \ "firstForename").formatNullable[String]
      ~ (__ \ "secondForename").formatNullable[String]
      ~ (__ \ "surname").formatNullable[String]
      ~ (__ \ "honours").formatNullable[String]
      ~ (__ \ "dateOfBirth").format[LocalDate]
      ~ (__ \ "nino").format[String]
      ~ (__ \ "address").formatNullable[AddressData]
      ~ (__ \ "crnIndicator").format[String])(
      IndividualDetailsData.apply,
      unlift { idd =>
        Some(
          Tuple9(
            idd.title,
            idd.firstForename,
            idd.secondForename,
            idd.surname,
            idd.honours,
            idd.dateOfBirth,
            idd.nino,
            idd.address,
            idd.crnIndicator
          )
        )
      }
    )
}
