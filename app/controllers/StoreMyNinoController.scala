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
import connectors.{AppleWalletConnector, GoogleWalletConnector,CitizenDetailsConnector}
import controllers.auth.requests.UserRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import util.AuditUtils
import views.html.StoreMyNinoView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StoreMyNinoController @Inject()(
                                       val citizenDetailsConnector: CitizenDetailsConnector,
                                       val appleWalletConnector: AppleWalletConnector,
                                       val googleWalletConnector: GoogleWalletConnector,
                                       authConnector: AuthConnector,
                                       auditService: AuditService,
                                       override val messagesApi: MessagesApi,
                                       getPersonDetailsAction: GetPersonDetailsAction,
                                       view: StoreMyNinoView
                                     )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {


  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) async {
    implicit request => {
      auditService.audit(AuditUtils.buildAuditEvent(request.personDetails, "ViewNinoLanding", frontendAppConfig.appName, None))
      val fullName: String = request.personDetails.person.fullName
      val nino: String = request.nino.map(_.formatted).getOrElse("")
      val googleIdf = googleWalletConnector.createGooglePass(fullName, nino)
      val appleIdf = appleWalletConnector.createApplePass(fullName, nino)

      googleIdf.flatMap { googleId =>
        appleIdf.map { appleId =>
          Ok(view(appleId.value, googleId.value, nino, isMobileDisplay(request)))
        }
      }
    }
  }

  private def isMobileDisplay(request: UserRequest[AnyContent]): Boolean = {
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
