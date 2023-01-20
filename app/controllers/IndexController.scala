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

package controllers

import config.FrontendAppConfig
import models.UserAnswers
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.routing.Router.RequestImplicits.WithHandlerDef
import play.api.{Configuration, Environment}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys
import views.html.IndexView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject()(
                                 view: IndexView,
                                 authConnector: AuthConnector,
                                 sessionRepository: SessionRepository
                               )(implicit
                                 config: Configuration,
                                 env: Environment,
                                 ec: ExecutionContext,
                                 cc: MessagesControllerComponents,
                                 frontendAppConfig: FrontendAppConfig
                               )
  extends FMNBaseController(authConnector) with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    Action.async { implicit request =>
      authorisedAsFMNUser { _ =>
        Future successful  {
           //val sessionId = request.session.get(SessionKeys.sessionId).get
           //sessionRepository.set(new UserAnswers(sessionId))
           Ok(view())
        }
      } (routes.IndexController.onPageLoad)
    }}


