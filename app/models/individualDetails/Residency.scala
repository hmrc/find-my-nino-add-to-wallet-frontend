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

final case class ResidencySequenceNumber(value: Int) extends AnyVal
object ResidencySequenceNumber {
  implicit val format: Format[ResidencySequenceNumber] = Json.valueFormat[ResidencySequenceNumber]
}
sealed trait ResidencyStatusFlag

object ResidencyStatusFlag {
  object UK     extends ResidencyStatusFlag
  object Abroad extends ResidencyStatusFlag

  implicit val reads: Reads[ResidencyStatusFlag] = JsPath
    .read[Int]
    .map {
      case 0 => UK
      case 1 => Abroad
    }
  implicit val writes: Writes[ResidencyStatusFlag] = WritesNumber[ResidencyStatusFlag] {
    case UK     => 0
    case Abroad => 1
  }
}

final case class Residency(
    residencySequenceNumber: ResidencySequenceNumber,
    dateLeavingUK:           Option[LocalDate],
    dateReturningUK:         Option[LocalDate],
    residencyStatusFlag:     ResidencyStatusFlag
)

object Residency {
  implicit val format: OFormat[Residency] = Json.format[Residency]
}
