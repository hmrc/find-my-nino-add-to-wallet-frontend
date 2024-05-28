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

package models

import base.SpecBase
import models.individualDetails.CountryCodeLookup

class CountryCodeLookupSpec extends SpecBase {

  val countryGB = 1
  val countryAustralia = 12
  val invalidCountry = 300

  "CountryCodeLookupSpec" - {
    "convertCodeToCountryName should return" - {
      "a valid country when valid code is supplied" in {
        val convertedCode = CountryCodeLookup.convertCodeToCountryName(countryGB)
        val expectedCountry = "GREAT BRITAIN"
        expectedCountry mustEqual convertedCode
      }

      "a valid country when a different valid code is supplied" in {
        val convertedCode = CountryCodeLookup.convertCodeToCountryName(countryAustralia)
        val expectedCountry = "AUSTRALIA"
        expectedCountry mustEqual convertedCode
      }

      "a country code when an invalid code is supplied" in {
        val convertedCode = CountryCodeLookup.convertCodeToCountryName(invalidCountry)
        val expectedCountry = "300"
        expectedCountry mustEqual convertedCode
      }
    }
  }
}