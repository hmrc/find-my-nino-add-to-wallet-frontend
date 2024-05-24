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

package util

object TestData {

  val allEnrolments: String = """
    |  "allEnrolments": [{
    |    "key": "HMRC-PT",
    |    "identifiers": [{ "key": "NINO", "value": "AA000003B" }],
    |    "state": "Activated"
    |   }]
    |""".stripMargin

  val NinoUser_With_CL50: String =
    s"""
      |{
      |	"nino": "AA000003B",
      |	"credentialRole": "User",
      | "credentialStrength": "strong",
      |	"internalId": "Int-8612ba91-5581-411d-9d32-fb2de937a565",
      | "confidenceLevel": 50,
      | "affinityGroup": "Individual",
      | $allEnrolments
      |}
      |""".stripMargin

  val NinoUser: String =
    s"""
       |{
       |	"nino": "AA000003B",
       |	"credentialRole": "User",
       | "credentialStrength": "strong",
       |	"internalId": "Int-8612ba91-5581-411d-9d32-fb2de937a565",
       | "confidenceLevel": 200,
       | "affinityGroup": "Individual",
       | $allEnrolments
       |}
       |""".stripMargin

  val NotFoundAccountError: String =
    """
      |{
      |		"status": 404,
      |		"description": "NOT_FOUND_CB_ACCOUNT - downstream service returned NOT_FOUND_IDENTIFIER, suggesting user does not have a child benefit account"
      |	}
      |""".stripMargin

  val LockedOutErrorResponse: String =
    """
      |{
      |		"status": 403,
      |		"description": "ClaimantIsLockedOut"
      |	}
      |""".stripMargin

}
