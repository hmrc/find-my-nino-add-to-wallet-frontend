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

import models.individualDetails.AddressType.ResidentialAddress
import models.json.WritesNumber
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

final case class NinoSuffix(value: String) extends AnyVal

object NinoSuffix {
  implicit val format: Format[NinoSuffix] = Json.valueFormat[NinoSuffix]
}

sealed trait CrnIndicator {
  val asString: String
}

object CrnIndicator {
  object False extends CrnIndicator {
    override val asString: String = "false"
  }
  object True  extends CrnIndicator {
    override val asString: String = "true"
  }

  implicit val reads: Reads[CrnIndicator] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[CrnIndicator] = WritesNumber[CrnIndicator] {
    case False => 0
    case True  => 1
  }
}

final case class IndividualDetails(
    ninoWithoutSuffix:  String,
    ninoSuffix:         Option[NinoSuffix],
    dateOfBirth:        LocalDate,
    crnIndicator:       CrnIndicator,
    nameList:           NameList,
    addressList:        AddressList
) {
  def fullIdentifier: String = s"""${ninoWithoutSuffix}${ninoSuffix.map(_.value).getOrElse("")}"""

  def getResidenceAddress: Option[Address] = addressList.getAddress
    .find(_.addressType.equals(ResidentialAddress))

  private def getCorrespondenceAddress: Option[Address] = addressList.getAddress
    .find(_.addressType.equals(AddressType.CorrespondenceAddress))

  private def getAddress: Option[Address] = getCorrespondenceAddress.orElse(getResidenceAddress)

  def getAddressData: Option[AddressData] = {
    getAddress.map(addr => AddressData(addr.addressLine1,
      addr.addressLine2,
      addr.addressLine3,
      addr.addressLine4,
      addr.addressLine5,
      addr.addressPostcode,
      CountryCodeLookup.convertCodeToCountryName(addr.countryCode.value),
      addr.addressStartDate,
      addr.addressType))

  }

  def getNino: String = ninoWithoutSuffix + ninoSuffix.map(_.value).getOrElse("")

  def getPreferredName = {
    nameList.name.find(_.nameType.equals(NameType.KnownAsName)).getOrElse(nameList.name.head)
  }


  private def getTitle: String =  {
    val maybeTitle: TitleType = getPreferredName
      .titleType.getOrElse(TitleType.NotKnown)
    maybeTitle match {
      case TitleType.Mr => "Mr"
      case TitleType.Mrs => "Mrs"
      case TitleType.Miss => "Miss"
      case TitleType.Ms => "Ms"
      case TitleType.Dr => "Dr"
      case TitleType.Rev => "Rev"
      case _ => ""
    }
  }

  private def getHonours: String = {
    getPreferredName.honours.map(_.value).getOrElse("")
  }

  def getFullName: String = {
    val name = getPreferredName
    List(getTitle, name.firstForename.toUpperCase(), name.secondForename.getOrElse("").toUpperCase(), name.surname.toUpperCase(), getHonours)
      .filter(_.nonEmpty)
      .mkString(" ")
  }

  def getInitialsName: String = {
    val name = getPreferredName
    List(getTitle, name.firstForename.toUpperCase().take(1), name.secondForename.getOrElse("").toUpperCase().take(1), name.surname.toUpperCase(), getHonours)
      .filter(_.nonEmpty)
      .mkString(" ")
  }

}

object IndividualDetails {

  implicit val reads: Format[IndividualDetails] =
    ((JsPath \ "details" \ "nino").format[String] ~
    (__ \ "details" \ "ninoSuffix").formatNullable[NinoSuffix].inmap(_.filter(_ != NinoSuffix(" ")), identity[Option[NinoSuffix]]) ~
    (__ \ "details" \ "dateOfBirth").format[LocalDate] ~
    (__ \ "details" \ "crnIndicator").format[CrnIndicator] ~
    (__ \ "nameList").format[NameList] ~
    (__ \ "addressList").format[AddressList])(IndividualDetails.apply, unlift(IndividualDetails.unapply))

}
