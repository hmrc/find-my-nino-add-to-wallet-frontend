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

package controllers.auth

import config.FrontendAppConfig
import controllers.bindable.Origin
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class AuthController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  frontendAppConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  def signout(continueUrl: Option[RedirectUrl], origin: Option[Origin]): Action[AnyContent] =
    Action {
      val safeUrl = continueUrl.flatMap { redirectUrl =>
        redirectUrl.getEither(OnlyRelative) match {
          case Right(safeRedirectUrl) =>
            Some(safeRedirectUrl.url)
          case _                      =>
            Some(frontendAppConfig.getFeedbackSurveyUrl(frontendAppConfig.defaultOrigin))
        }
      }
      safeUrl
        .orElse(origin.map(frontendAppConfig.getFeedbackSurveyUrl))
        .fold(BadRequest("Missing origin")) { (url: String) =>
          Redirect(frontendAppConfig.getBasGatewayFrontendSignOutUrl(url))
        }
    }
}
