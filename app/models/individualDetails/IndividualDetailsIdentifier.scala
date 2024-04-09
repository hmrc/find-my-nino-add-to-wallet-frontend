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

import play.api.libs.json.{Format, JsPath, Json, Reads, Writes}

import scala.util.matching.Regex

sealed trait IndividualDetailsIdentifier {
  val value: String
}
final case class IndividualDetailsNino(value: String) // NINO and CRN are used interchangeably
    extends IndividualDetailsIdentifier {
  def withoutSuffix = value.take(8)
}
final case class ChildReferenceNumber(value: String) extends IndividualDetailsIdentifier
final case class TemporaryReferenceNumber(value: String) extends IndividualDetailsIdentifier

object IndividualDetailsIdentifier {

  val NinoAndCRNRegexWithAndWithoutSuffix: Regex =
    """^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D\s]?$""".r
  val CRNRegexWithNoSuffix: Regex =
    """^(?:[ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]|B[A-CEHJ-NPR-TW-Z]|G[ACEGHJ-NPR-TW-Z]|[KT][A-CEGHJ-MPR-TW-Z]|N[A-CEGHJL-NPR-SW-Z]|Z[A-CEGHJ-NPR-TW-Y])[0-9]{6}$""".r
  val TRNRegex: Regex = """^[0-9]{2}[A-Z]{1}[0-9]{5}$""".r

  implicit val reads: Reads[IndividualDetailsIdentifier] = JsPath.read[String].map {
    case NinoAndCRNRegexWithAndWithoutSuffix(nino) => IndividualDetailsNino(nino)
    case CRNRegexWithNoSuffix(crn)                 => ChildReferenceNumber(crn)
    case TRNRegex(trn)                             => TemporaryReferenceNumber(trn)
    case _                                         => throw new RuntimeException("Unable to parse ChildBenefitIdentifier")
  }

  implicit val writes: Writes[IndividualDetailsIdentifier] = JsPath.write[String].contramap[IndividualDetailsIdentifier] {
    case IndividualDetailsNino(nino) => nino
    case ChildReferenceNumber(crn)     => crn
    case TemporaryReferenceNumber(trn) => trn
  }
}

object IndividualDetailsNino {
  implicit val format: Format[IndividualDetailsNino] = Json.valueFormat[IndividualDetailsNino]
}

object ChildReferenceNumber {
  implicit val format: Format[ChildReferenceNumber] = Json.valueFormat[ChildReferenceNumber]
}

object TemporaryReferenceNumber {
  implicit val tempFormat: Format[TemporaryReferenceNumber] = Json.valueFormat[TemporaryReferenceNumber]
}
