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
import connectors.{CitizenDetailsConnector, StoreMyNinoConnector}
import controllers.auth.requests.UserRequest
import play.api.{Configuration, Environment}

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import util.AuditUtils
import views.html.{GoogleWalletView, PassIdNotFoundView, QRCodeNotFoundView}

import scala.concurrent.ExecutionContext

class GoogleWalletController @Inject()(val citizenDetailsConnector: CitizenDetailsConnector,
                                       override val messagesApi: MessagesApi,
                                       authConnector: AuthConnector,
                                       view: GoogleWalletView,
                                       findMyNinoServiceConnector: StoreMyNinoConnector,
                                       getPersonDetailsAction: GetPersonDetailsAction,
                                       auditService: AuditService,
                                       passIdNotFoundView: PassIdNotFoundView,
                                       qrCodeNotFoundView: QRCodeNotFoundView
                                      )(implicit config: Configuration,
                                        env: Environment,
                                        ec: ExecutionContext,
                                        cc: MessagesControllerComponents,
                                        frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) async {
    implicit request => {
      auditService.audit(AuditUtils.buildAuditEvent(request.personDetails, "ViewWalletPage", frontendAppConfig.appName, Some("Google")))
      for {
        pId: Some[String] <- findMyNinoServiceConnector.createGooglePass(
          request.personDetails.person.fullName,
          request.nino.map(_.formatted).getOrElse(""))
      } yield Ok(view(pId.value, isMobileDisplay(request)))
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

  def getGooglePass(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction).async {
    implicit request => {
      authorisedAsFMNUser { _ =>
        findMyNinoServiceConnector.getGooglePassUrl(passId).map {
          case Some(data) =>
            request.getQueryString("qr-code") match {
              case Some("true") => auditService.audit(AuditUtils.buildAuditEvent(request.personDetails,
                "AddNinoToWalletFromQRCode", frontendAppConfig.appName, Some("Google")))
              case _ => auditService.audit(AuditUtils.buildAuditEvent(request.personDetails,
                "AddNinoToWallet", frontendAppConfig.appName, Some("Google")))
            }
            Redirect(data)
          case _ => NotFound(passIdNotFoundView())
        }
      }(loginContinueUrl)
    }
  }

  def getGooglePassQrCode(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction).async {
    implicit request => {
      authorisedAsFMNUser { _ =>
        findMyNinoServiceConnector.getGooglePassQrCode(passId).map {
          case Some(data) =>
            auditService.audit(AuditUtils.buildAuditEvent(request.personDetails, "DisplayQRCode", frontendAppConfig.appName, Some("Google")))
            Ok(data)
          case _ => NotFound(qrCodeNotFoundView())
        }
      }(loginContinueUrl)
    }
  }
}
