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

import config.{ConfigDecorator, FrontendAppConfig}
import connectors.{CitizenDetailsConnector, StoreMyNinoConnector}
import controllers.auth.requests.UserRequest
import play.api.Configuration

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.{AppleWalletView, ErrorTemplate, PassIdNotFoundView, QRCodeNotFoundView}
import play.api.Environment
import util.AuditUtils

import scala.concurrent.{ExecutionContext, Future}

class AppleWalletController @Inject()(val citizenDetailsConnector: CitizenDetailsConnector,
                                      findMyNinoServiceConnector: StoreMyNinoConnector,
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      auditService: AuditService,
                                      view: AppleWalletView,
                                      errorTemplate: ErrorTemplate,
                                      getPersonDetailsAction: GetPersonDetailsAction,
                                      passIdNotFoundView: PassIdNotFoundView,
                                      qrCodeNotFoundView: QRCodeNotFoundView
                                      )(implicit config: Configuration,
                                        configDecorator: ConfigDecorator,
                                        env: Environment,
                                        ec: ExecutionContext,
                                        cc: MessagesControllerComponents,
                                        frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad
  private val passFileName = "National-Insurance-number-card.pkpass"

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) async {
    implicit request => {
      request.personDetails match {
        case Some(pd) =>
          auditService.audit(AuditUtils.buildAuditEvent(pd, "ViewAppleWalletPage", configDecorator.appName, None))
          for {
            pId: Some[String] <- findMyNinoServiceConnector.createApplePass(pd.person.fullName, request.nino.map(_.formatted).getOrElse(""))
          } yield Ok(view(pId.value, isMobileDisplay(request)))
        case None =>
          Future(NotFound(errorTemplate("Details not found", "Your details were not found.", "Your details were not found, please try again later.")))
      }
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

  def getPassCard(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction).async {
    implicit request => {
      authorisedAsFMNUser { _ =>
        findMyNinoServiceConnector.getApplePass(passId).map {
          case Some(data) =>
            request.getQueryString("qr-code") match {
              case Some("true") => auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get,
                "AddNinoToWalletFromQRCode", configDecorator.appName, Some("Apple")))
              case _ => auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get,
                "AddNinoToWallet", configDecorator.appName, Some("Apple")))
            }
            Ok(data).withHeaders("Content-Disposition" -> s"attachment; filename=$passFileName")
          case _ => NotFound(passIdNotFoundView())
        }
      }(loginContinueUrl)
    }
  }

  def getQrCode(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction).async {
    implicit request => {
      authorisedAsFMNUser { _ =>
        findMyNinoServiceConnector.getQrCode(passId).map {
          case Some(data) =>
            auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get, "DisplayQRCode", configDecorator.appName, Some("Apple")))
            Ok(data).withHeaders("Content-Disposition" -> s"attachment; filename=$passFileName")
          case _ => NotFound(qrCodeNotFoundView())
        }
      }(loginContinueUrl)
    }
  }
}
