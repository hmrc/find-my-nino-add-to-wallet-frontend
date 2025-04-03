/*
 * Copyright 2025 HM Revenue & Customs
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

import base.SpecBase

class AuditUtilsSpec extends SpecBase {
  
  "getLanguageFromCookieStr" - {
    "return the language in header carrier" in {
      val lang = "languageChoice"
      AuditUtils.getLanguageFromCookieStr(hc.copy(otherHeaders = Seq("Cookie" -> s"PLAY_LANG=$lang"))) mustBe lang
    }
    "default to en when new value in hc" in {
      AuditUtils.getLanguageFromCookieStr(hc.copy(otherHeaders = Seq("Cookie" -> "no language"))) mustBe "en"
    }
  }

  "getUserDevice" - {
    "return the correct user device when value is in header carrier" in {
      val userDevice = Map("other info iPhone" -> "iOS", "other info Android" -> "Android", "other info unknown" -> "")
      userDevice.map {
        pair =>
          AuditUtils.getUserDevice(hc.copy(otherHeaders = Seq("User-Agent" -> pair._1))) mustBe pair._2
      }
    }
  }

  "getUserAgent" - {
    "return the userAgent" in {
      val userAgent = "userAgent"
      AuditUtils.getUserAgent(hc.copy(otherHeaders = Seq("User-Agent" -> userAgent))) mustBe userAgent
    }
  }
}
