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

sealed trait AccountStatusType

object AccountStatusType {
  case object FullLive                 extends AccountStatusType
  case object PseudoIomPre86           extends AccountStatusType
  case object FullIomPost86            extends AccountStatusType
  case object FullCancelled            extends AccountStatusType
  case object FullAmalgamated          extends AccountStatusType
  case object FullAdministrative       extends AccountStatusType
  case object PseudoWeeded             extends AccountStatusType
  case object PseudoAmalgamated        extends AccountStatusType
  case object PseudoOther              extends AccountStatusType
  case object Redundant                extends AccountStatusType
  case object ConversionRejection      extends AccountStatusType
  case object Redirected               extends AccountStatusType
  case object PayeTemporary            extends AccountStatusType
  case object AmalgamatedPayeTemporary extends AccountStatusType
  case object NotKnown                 extends AccountStatusType

  implicit val reads: Reads[AccountStatusType] = JsPath
    .read[Int]
    .map {
      case 0  => FullLive
      case 1  => PseudoIomPre86
      case 2  => FullIomPost86
      case 3  => FullCancelled
      case 4  => FullAmalgamated
      case 5  => FullAdministrative
      case 6  => PseudoWeeded
      case 7  => PseudoAmalgamated
      case 8  => PseudoOther
      case 9  => Redundant
      case 10 => ConversionRejection
      case 11 => Redirected
      case 12 => PayeTemporary
      case 13 => AmalgamatedPayeTemporary
      case 99 => NotKnown
    }

  implicit val writes: Writes[AccountStatusType] = WritesNumber[AccountStatusType] {
    case FullLive                 => 0
    case PseudoIomPre86           => 1
    case FullIomPost86            => 2
    case FullCancelled            => 3
    case FullAmalgamated          => 4
    case FullAdministrative       => 5
    case PseudoWeeded             => 6
    case PseudoAmalgamated        => 7
    case PseudoOther              => 8
    case Redundant                => 9
    case ConversionRejection      => 10
    case Redirected               => 11
    case PayeTemporary            => 12
    case AmalgamatedPayeTemporary => 13
    case NotKnown                 => 99
  }
}

sealed trait DateOfBirthStatus

object DateOfBirthStatus {
  object Unverified    extends DateOfBirthStatus
  object Verified      extends DateOfBirthStatus
  object NotKnown      extends DateOfBirthStatus
  object CoegConfirmed extends DateOfBirthStatus

  implicit val reads: Reads[DateOfBirthStatus] = JsPath
    .read[Int]
    .map {
      case 0 => Unverified
      case 1 => Verified
      case 2 => NotKnown
      case 3 => CoegConfirmed

    }
  implicit val writes: Writes[DateOfBirthStatus] = WritesNumber[DateOfBirthStatus] {
    case Unverified    => 0
    case Verified      => 1
    case NotKnown      => 2
    case CoegConfirmed => 3
  }
}

sealed trait DateOfDeathStatus

object DateOfDeathStatus {

  object Unverified    extends DateOfDeathStatus
  object Verified      extends DateOfDeathStatus
  object NotKnown      extends DateOfDeathStatus
  object CoegConfirmed extends DateOfDeathStatus

  implicit val reads: Reads[DateOfDeathStatus] = JsPath
    .read[Int]
    .map {
      case 0 => Unverified
      case 1 => Verified
      case 2 => NotKnown
      case 3 => CoegConfirmed

    }
  implicit val writes: Writes[DateOfDeathStatus] = WritesNumber[DateOfDeathStatus] {
    case Unverified    => 0
    case Verified      => 1
    case NotKnown      => 2
    case CoegConfirmed => 3
  }
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
    accountStatusType:  Option[AccountStatusType],
    dateOfEntry:        Option[LocalDate],
    dateOfBirth:        LocalDate,
    dateOfBirthStatus:  Option[DateOfBirthStatus],
    dateOfDeath:        Option[LocalDate],
    dateOfDeathStatus:  Option[DateOfDeathStatus],
    dateOfRegistration: Option[LocalDate],
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

  def knownAsOrHead(list: NameList): Name = {
    list.name.find(_.nameType.equals(NameType.KnownAsName)).getOrElse(list.name.head)
  }

  def getFirstForename: String = knownAsOrHead(nameList).firstForename.value
  private def getSecondForename: String = knownAsOrHead(nameList).secondForename.getOrElse(SecondForename("")).value
  def getLastName: String = knownAsOrHead(nameList).surname.value

  private def getTitle: String =  {
    val maybeTitle: TitleType = knownAsOrHead(nameList)
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
    knownAsOrHead(nameList).honours.map(_.value).getOrElse("")
  }

  def getFullName: String = {
    List(getTitle, getFirstForename.toUpperCase(), getSecondForename.toUpperCase(), getLastName.toUpperCase(), getHonours)
      .filter(_.nonEmpty)
      .mkString(" ")
  }

  def getInitialsName: String = {
    List(getTitle, getFirstForename.toUpperCase().take(1), getSecondForename.toUpperCase().take(1), getLastName.toUpperCase(), getHonours)
      .filter(_.nonEmpty)
      .mkString(" ")
  }

}

object IndividualDetails {

  implicit val reads: Format[IndividualDetails] =
    ((JsPath \ "details" \ "nino").format[String] ~
    (__ \ "details" \ "ninoSuffix").formatNullable[NinoSuffix].inmap(_.filter(_ != NinoSuffix(" ")), identity[Option[NinoSuffix]]) ~
    (__ \ "details" \ "accountStatusType").formatNullable[AccountStatusType] ~
    (__ \ "details" \ "dateOfEntry").formatNullable[LocalDate] ~
    (__ \ "details" \ "dateOfBirth").format[LocalDate] ~
    (__ \ "details" \ "dateOfBirthStatus").formatNullable[DateOfBirthStatus] ~
    (__ \ "details" \ "dateOfDeath").formatNullable[LocalDate] ~
    (__ \ "details" \ "dateOfDeathStatus").formatNullable[DateOfDeathStatus] ~
    (__ \ "details" \ "dateOfRegistration").formatNullable[LocalDate] ~
    (__ \ "details" \ "crnIndicator").format[CrnIndicator] ~
    (__ \ "nameList").format[NameList] ~
    (__ \ "addressList").format[AddressList])(IndividualDetails.apply, unlift(IndividualDetails.unapply))

}
