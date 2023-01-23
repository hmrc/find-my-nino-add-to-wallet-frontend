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

package util

import javax.inject.Inject
import javax.xml.transform.{Source, URIResolver}
import play.api.Environment
import play.api.Logging

class DefaultStylesheetResourceStreamResolver @Inject()(val environment: Environment) extends StylesheetResourceStreamResolver

trait StylesheetResourceStreamResolver extends URIResolver with BaseResourceStreamResolver with Logging {
  val environment: Environment

  override def resolve(href: String, base: String): Source = {
    logger.info("[StylesheetResolver] Stylesheet location to convert " + href)

    resolvePath(href.substring(href.lastIndexOf("/pdf") + 1))
  }
}