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

import models.json.WritesNumber
import play.api.libs.json._

import java.time.LocalDate
final case class AddressSequenceNumber(value: Int) extends AnyVal

object AddressSequenceNumber {
  implicit val format: Format[AddressSequenceNumber] = Json.valueFormat[AddressSequenceNumber]
}

sealed trait AddressSource

object AddressSource {
  case object NotKnown extends AddressSource
  case object Customer extends AddressSource
  case object Relative extends AddressSource
  case object Employer extends AddressSource
  case object InlandRevenue extends AddressSource
  case object OtherGovernmentDepartment extends AddressSource
  case object OtherThirdParty extends AddressSource
  case object Cutover extends AddressSource
  case object RealTimeInformation extends AddressSource
  case object PersonalAccountUser extends AddressSource

  implicit val reads: Reads[AddressSource]   = JsPath
    .read[Int]
    .map {
      case 0 => NotKnown
      case 1 => Customer
      case 2 => Relative
      case 3 => Employer
      case 4 => InlandRevenue
      case 5 => OtherGovernmentDepartment
      case 6 => OtherThirdParty
      case 7 => Cutover
      case 8 => RealTimeInformation
      case 9 => PersonalAccountUser
    }
  implicit val writes: Writes[AddressSource] = WritesNumber[AddressSource] {
    case NotKnown                  => 0
    case Customer                  => 1
    case Relative                  => 2
    case Employer                  => 3
    case InlandRevenue             => 4
    case OtherGovernmentDepartment => 5
    case OtherThirdParty           => 6
    case Cutover                   => 7
    case RealTimeInformation       => 8
    case PersonalAccountUser       => 9
  }
}

final case class CountryCode(value: Int) extends AnyVal

object CountryCode {
  implicit val format: Format[CountryCode] = Json.valueFormat[CountryCode]
}
sealed trait AddressType

object AddressType {
  case object ResidentialAddress extends AddressType
  case object CorrespondenceAddress extends AddressType

  implicit val reads: Reads[AddressType]   = JsPath
    .read[Int]
    .map {
      case 1 => ResidentialAddress
      case 2 => CorrespondenceAddress
    }
  implicit val writes: Writes[AddressType] = WritesNumber[AddressType] {
    case ResidentialAddress    => 1
    case CorrespondenceAddress => 2
  }
}

sealed trait AddressStatus

object AddressStatus {
  case object NotDlo extends AddressStatus
  case object Dlo extends AddressStatus
  case object Nfa extends AddressStatus

  implicit val reads: Reads[AddressStatus]   = JsPath
    .read[Int]
    .map {
      case 0 => NotDlo
      case 1 => Dlo
      case 2 => Nfa
    }
  implicit val writes: Writes[AddressStatus] = WritesNumber[AddressStatus] {
    case NotDlo => 0
    case Dlo    => 1
    case Nfa    => 2
  }

}
final case class AddressStartDate(value: Int) extends AnyVal

object AddressStartDate {
  implicit val format: Format[AddressStartDate] = Json.valueFormat[AddressStartDate]
}
final case class VpaMail(value: Int) extends AnyVal

object VpaMail {
  implicit val format: Format[VpaMail] = Json.valueFormat[VpaMail]
}

final case class DeliveryInfo(value: String) extends AnyVal

object DeliveryInfo {
  implicit val format: Format[DeliveryInfo] = Json.valueFormat[DeliveryInfo]
}

final case class PafReference(value: String) extends AnyVal

object PafReference {
  implicit val format: Format[PafReference] = Json.valueFormat[PafReference]
}

final case class Address(
  addressSequenceNumber: AddressSequenceNumber,
  addressSource: Option[AddressSource],
  countryCode: CountryCode,
  addressType: AddressType,
  addressStatus: Option[AddressStatus],
  addressStartDate: LocalDate,
  addressEndDate: Option[LocalDate],
  addressLastConfirmedDate: Option[LocalDate],
  vpaMail: Option[VpaMail],
  deliveryInfo: Option[DeliveryInfo],
  pafReference: Option[PafReference],
  addressLine1: AddressLine,
  addressLine2: AddressLine,
  addressLine3: Option[AddressLine],
  addressLine4: Option[AddressLine],
  addressLine5: Option[AddressLine],
  addressPostcode: Option[AddressPostcode]
)

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}
