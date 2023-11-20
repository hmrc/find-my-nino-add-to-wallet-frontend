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
import connectors.GovUKWalletSMNConnector
import controllers.auth.requests.UserRequest
import play.api.{Configuration, Environment}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.{ErrorTemplate, GovUKWalletView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GovUKWalletController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      view: GovUKWalletView,
                                      errorTemplate: ErrorTemplate,
                                      getPersonDetailsAction: GetPersonDetailsAction,
                                      govUKWalletSMNConnector: GovUKWalletSMNConnector
                                    )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig)
  extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) async {
    implicit request =>
      if (frontendAppConfig.govukWalletEnabled) {
        request.personDetails match {
          case Some(pd) =>
            for {
              pId: Some[String] <- govUKWalletSMNConnector.createGovUKPass(
                pd.person.givenName,
                pd.person.familyName,
                request.nino.map(_.formatted).getOrElse(""))
            } yield Ok(view(pId.value, isMobileDisplay(request)))
          case None =>
            Future(NotFound(errorTemplate("Details not found", "Your details were not found.", "Your details were not found, please try again later.")))
        }
      }
      else {
        Future(Redirect(routes.UnauthorisedController.onPageLoad))
      }

  }

  private def isMobileDisplay(request: UserRequest[AnyContent]): Boolean = {
    // Display wallet options differently on mobile to pc
    val strUserAgent = request.headers.get("http_user_agent")
      .getOrElse(request.headers.get("User-Agent")
        .getOrElse(""))

    config.get[String]("mobileDeviceDetectionRegexStr").r
      .findFirstMatchIn(strUserAgent) match {
      case Some(_) => true
      case None => false
    }
  }

}
