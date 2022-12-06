/*
 * Copyright 2022 HM Revenue & Customs
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

case class PersonDetails(
                          person: Person,
                          address: Option[Address],
                          correspondenceAddress: Option[Address]
                        )

object PersonDetails {
  implicit val formats = Json.format[PersonDetails]

  // temporary dummy data
  var personDetails: PersonDetails = PersonDetails(
    Person(
      Option("Joanne"),
      Option("Rachel"),
      Option("Bloggs"),
      None,
      Option("Ms"),
      None,
      Option("QQ 12 34 56 A")
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
    ))
  )

}
