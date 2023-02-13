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

package config

import base.SpecBase
import org.mockito.MockitoSugar.when
import org.mockito.WhenMacro
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Lang
import play.api.inject.NewInstanceInjector.instanceOf
import org.scalatest.BeforeAndAfter
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier.Config
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig


class ConfigDecoratorSpec extends SpecBase with MockitoSugar{
  val mockConfigDecorator = mock[ConfigDecorator]
  when(mockConfigDecorator.languageMap).thenReturn(Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  ))

  "Methods tests" - {
    "languageTranslationEnabled must return Boolean" in
        mockConfigDecorator.languageTranslationEnabled.isInstanceOf[Boolean]
    }

    "languageMap" in {
      mockConfigDecorator.languageMap.isInstanceOf[Map[String, Lang]]
    }

  "languageMap count is correct" in {
    mockConfigDecorator.languageMap.size.equals(2)
  }

  "languageMap returns eng language" in {
    mockConfigDecorator.languageMap.get("en").equals(Lang("en"))
    }

  "languageMap returns welsh language" in {
    mockConfigDecorator.languageMap.get("cy").equals(Lang("cy"))
  }

  "languageMap returns correct map" in {
    mockConfigDecorator.languageMap.equals(Map(
      "en" -> Lang("en"),
      "cy" -> Lang("cy")
    ))
  }


   "timeout" in {
      mockConfigDecorator.timeout.isInstanceOf[Int]
    }

    "signOutUrl" in {
      mockConfigDecorator.signOutUrl.isInstanceOf[String]
    }

    "loginUrl" in {
      mockConfigDecorator.loginUrl.isInstanceOf[String]
    }

    "host" in {
      mockConfigDecorator.host.isInstanceOf[String]
    }

    "cacheTtl" in {
      mockConfigDecorator.cacheTtl.isInstanceOf[Int]
    }

    "appName" in {
      mockConfigDecorator.appName.isInstanceOf[String]
    }

    "countdown" in {
      mockConfigDecorator.countdown.isInstanceOf[Int]
    }

    "findMyNinoServiceUrl" in {
      mockConfigDecorator.findMyNinoServiceUrl.isInstanceOf[String]
    }

    "exitSurveyUrl" in {
      mockConfigDecorator.exitSurveyUrl.isInstanceOf[String]
    }

    "loginContinueUrl" in {
      mockConfigDecorator.loginContinueUrl.isInstanceOf[String]
    }

    "container" in {
      mockConfigDecorator.gtmContainer.isInstanceOf[String]
    }

}
