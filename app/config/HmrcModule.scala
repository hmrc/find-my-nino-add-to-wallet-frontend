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

import controllers.actions.*
import org.apache.fop.apps.FopFactory
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import util.{BaseResourceStreamResolver, DefaultFopURIResolver, DefaultResourceStreamResolver, FopURIResolver}
import views.html.templates.{LayoutProvider, NewLayoutProvider}

import java.time.{Clock, ZoneOffset}

class HmrcModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind[IdentifierAction].to(classOf[SessionIdentifierAction]),
      bind[Clock].toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC)),
      bind[FopFactory].toProvider(classOf[FopFactoryProvider]),
      bind[FopURIResolver].to(classOf[DefaultFopURIResolver]),
      bind[BaseResourceStreamResolver].to(classOf[DefaultResourceStreamResolver]),
      bind[LayoutProvider].to(classOf[NewLayoutProvider])
    )
}
