/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

case class PersonDetailsDummy(
                               person: Person,
                               address: Option[Address],
                               correspondenceAddress: Option[Address]
                             )
/*
Person(
                   firstName: Option[String],
                   middleName: Option[String],
                   lastName: Option[String],
                   initials: Option[String],
                   title: Option[String],
                   honours: Option[String],
                   sex: Option[String],
                   dateOfBirth: Option[LocalDate],
                   nino: Option[Nino]
                 )
 */
object PersonDetailsDummy {
  implicit val formats = Json.format[PersonDetails]

  // temporary dummy data
  var personDetailsDummy: PersonDetailsDummy = PersonDetailsDummy(
    Person(
      Option("Joanne"),
      Option("Rachel"),
      Option("Bloggs"),
      None,
      Option("Ms"),
      None,
      None,
      Option[LocalDate](LocalDate.of(1972,1,1)),
      Option(Nino("AA BB CC")),
    ),
    Option(Address(
      Option("10 Long Lane"),
      Option("Leeds"),
      None,
      None,
      None,
      Option("LS1 1AB"),
      Option("England"),
      None,
      None,
      None,
      false
    )),
    Option(Address(
      Option("10 Long Lane"),
      Option("Leeds"),
      None,
      None,
      None,
      Option("LS1 1AB"),
      Option("England"),
      None,
      None,
      None,
      false
    )
  ))

}
