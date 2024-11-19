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


sealed trait NameType
object NameType {
  object RealName    extends NameType
  object KnownAsName extends NameType

  implicit val reads: Reads[NameType] = JsPath
    .read[Int]
    .map {
      case 1 => RealName
      case 2 => KnownAsName
    }

  implicit val writes: Writes[NameType] = WritesNumber[NameType]({
    case RealName    => 1
    case KnownAsName => 2
  })

  implicit val format: Format[NameType] = Format[NameType](reads, writes)
}

sealed trait TitleType

object TitleType {
  object NotKnown extends TitleType
  object Mr       extends TitleType
  object Mrs      extends TitleType
  object Miss     extends TitleType
  object Ms       extends TitleType
  object Dr       extends TitleType
  object Rev      extends TitleType
  implicit val reads: Reads[TitleType] = JsPath
    .read[Int]
    .map {
      case 0 => NotKnown
      case 1 => Mr
      case 2 => Mrs
      case 3 => Miss
      case 4 => Ms
      case 5 => Dr
      case 6 => Rev
    }
  implicit val writes: Writes[TitleType] = WritesNumber[TitleType] {
    case NotKnown => 0
    case Mr       => 1
    case Mrs      => 2
    case Miss     => 3
    case Ms       => 4
    case Dr       => 5
    case Rev      => 6
  }

}

final case class RequestedName(value: String) extends AnyVal
object RequestedName {
  implicit val format: Format[RequestedName] = Json.valueFormat[RequestedName]
}
final case class NameStartDate(value: LocalDate) extends AnyVal
object NameStartDate {
  implicit val format: Format[NameStartDate] = Json.valueFormat[NameStartDate]
}
final case class NameEndDate(value: LocalDate) extends AnyVal
object NameEndDate {
  implicit val format: Format[NameEndDate] = Json.valueFormat[NameEndDate]
}
final case class OtherTitle(value: String) extends AnyVal
object OtherTitle {
  implicit val format: Format[OtherTitle] = Json.valueFormat[OtherTitle]
}
final case class Honours(value: String) extends AnyVal
object Honours {
  implicit val format: Format[Honours] = Json.valueFormat[Honours]
}

final case class Name(
    nameSequenceNumber: Int,
    nameType:           NameType,
    titleType:          Option[TitleType],
    requestedName:      Option[RequestedName],
    otherTitle:         Option[OtherTitle],
    honours:            Option[Honours],
    firstForename:      String,
    secondForename:     Option[String],
    surname:            String
)

object Name {
  implicit val format: OFormat[Name] = Json.format[Name]
}
