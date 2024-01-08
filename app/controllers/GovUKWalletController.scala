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

import config.FrontendAppConfig
import connectors.GovUKWalletSMNConnector
import controllers.auth.requests.UserRequest
import models.{GovUkPassCreateResponse, PersonDetails}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import util.AuditUtils
import views.html.{ErrorTemplate, GovUKWalletView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GovUKWalletController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      view: GovUKWalletView,
                                      errorTemplate: ErrorTemplate,
                                      getPersonDetailsAction: GetPersonDetailsFromAuthAction,
                                      govUKWalletSMNConnector: GovUKWalletSMNConnector,
                                      auditService: AuditService
                                    )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig)
  extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) async {
    implicit request =>
      auditService.audit(AuditUtils.buildAuditEvent(request.personDetails, "ViewWalletPage", frontendAppConfig.appName, Some("GovUk")))
      if (frontendAppConfig.govukWalletEnabled) {
        request.personDetails match {
          case pd @ PersonDetails(person, _, _) =>
            for {
              pId: Some[GovUkPassCreateResponse] <- govUKWalletSMNConnector.createGovUKPass(
                person.title.getOrElse("Mr"),
                person.givenName,
                person.familyName,
                request.nino.map(_.formatted).getOrElse(""))
            } yield Ok(view(
              pId.value.url,
              pId.value.bytes,
              isMobileDisplay(request)))
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
