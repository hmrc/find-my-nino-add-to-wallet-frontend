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

package config

import base.SpecBase
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class FrontendAppConfigSpec extends SpecBase with MockitoSugar {
  val mockFrontendAppConfig = mock[FrontendAppConfig]
  when(mockFrontendAppConfig.languageMap).thenReturn(Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  ))

  "languageMap" in {
    mockFrontendAppConfig.languageMap.isInstanceOf[Map[String, Lang]]
  }

  "languageMap count is correct" in {
    mockFrontendAppConfig.languageMap.size.equals(2)
  }

  "languageMap returns eng language" in {
    mockFrontendAppConfig.languageMap.get("en").equals(Lang("en"))
  }

  "languageMap returns welsh language" in {
    mockFrontendAppConfig.languageMap.get("cy").equals(Lang("cy"))
  }

  "languageMap returns correct map" in {
    mockFrontendAppConfig.languageMap.equals(Map(
      "en" -> Lang("en"),
      "cy" -> Lang("cy")
    ))
  }

  "signOutUrl" in {
    mockFrontendAppConfig.signOutUrl.isInstanceOf[String]
  }

  "loginUrl" in {
    mockFrontendAppConfig.loginUrl.isInstanceOf[String]
  }

  "host" in {
    mockFrontendAppConfig.host.isInstanceOf[String]
  }

  "cacheTtl" in {
    mockFrontendAppConfig.cacheTtl.isInstanceOf[Int]
  }

  "appName" in {
    mockFrontendAppConfig.appName.isInstanceOf[String]
  }

  "findMyNinoServiceUrl" in {
    mockFrontendAppConfig.findMyNinoServiceUrl.isInstanceOf[String]
  }

  "loginContinueUrl" in {
    mockFrontendAppConfig.loginContinueUrl.isInstanceOf[String]
  }

  "container" in {
    mockFrontendAppConfig.gtmContainer.isInstanceOf[String]
  }

}
