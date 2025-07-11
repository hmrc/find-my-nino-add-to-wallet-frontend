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

package controllers

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.FandFConnector
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.public.SessionTimeoutView

import scala.concurrent.{ExecutionContext, Future}

class PublicController @Inject() (
  sessionTimeoutView: SessionTimeoutView,
  authConnector: AuthConnector,
  fandFConnector: FandFConnector
)(implicit
  frontendAppConfig: FrontendAppConfig,
  cc: MessagesControllerComponents,
  config: Configuration,
  env: Environment,
  ec: ExecutionContext
) extends FMNBaseController(authConnector, fandFConnector)
    with I18nSupport {

  def sessionTimeout: Action[AnyContent] = Action.async { implicit request =>
    Future.successful {
      Ok(sessionTimeoutView())
    }
  }

}
