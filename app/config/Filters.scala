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

import play.api.http.{EnabledFilters, HttpFilters}
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.sca.filters.WrapperDataFilter

import javax.inject.{Inject, Singleton};

@Singleton
class Filters @Inject()(
                         defaultFilters: EnabledFilters,
                         allowListFilter: AllowlistFilter,
                         wrapperDataFilter: WrapperDataFilter,
                         appConfig: FrontendAppConfig
                       ) extends HttpFilters {

  val allowListFilterEnabled: Boolean = allowListFilter.allowlist.nonEmpty

  override val filters: Seq[EssentialFilter] = {
    val wrapperFilterOpt = if (appConfig.SCAWrapperEnabled) Seq(wrapperDataFilter) else Seq.empty
    val allowListFilterOpt = if (allowListFilterEnabled) defaultFilters.filters :+ allowListFilter else defaultFilters.filters

    wrapperFilterOpt ++ allowListFilterOpt
  }
}
