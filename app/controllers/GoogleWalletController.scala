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

import com.google.auth.oauth2.GoogleCredentials
import config.{ConfigDecorator, FrontendAppConfig}
import connectors.{CitizenDetailsConnector, StoreMyNinoConnector}
import controllers.auth.requests.UserRequest
import play.api.{Configuration, Environment}

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import util.{AuditUtils, GoogleCredentialsHelper, GoogleCredentialsSerializer}
import views.html.{ErrorTemplate, GoogleWalletView}

import java.io.ByteArrayInputStream
import java.util.{Base64, Collections}
import scala.concurrent.{ExecutionContext, Future}

class GoogleWalletController @Inject()(val citizenDetailsConnector: CitizenDetailsConnector,
                                       override val messagesApi: MessagesApi,
                                       authConnector: AuthConnector,
                                       view: GoogleWalletView,
                                       findMyNinoServiceConnector: StoreMyNinoConnector,
                                       errorTemplate: ErrorTemplate,
                                       getPersonDetailsAction: GetPersonDetailsAction,
                                       auditService: AuditService,
                                       googleCredentialsHelper: GoogleCredentialsHelper,
                                      )(implicit config: Configuration,
                                        configDecorator: ConfigDecorator,
                                        env: Environment,
                                        ec: ExecutionContext,
                                        cc: MessagesControllerComponents,
                                        frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) async {
    implicit request => {
      request.personDetails match {
        case Some(pd) =>
          auditService.audit(AuditUtils.buildAuditEvent(pd, "ViewGoogleWalletPage", configDecorator.appName))
          for {
            pId: Some[String] <- findMyNinoServiceConnector.createGooglePassWithCredentials(
              pd.person.fullName,
              request.nino.map(_.formatted).getOrElse(""),
              googleCredentialsHelper.createGoogleCredentials(configDecorator.googleKey))
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

  def getGooglePass(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction).async {
    implicit request => {
      authorisedAsFMNUser { _ =>
        findMyNinoServiceConnector.getGooglePassUrl(passId).map {
          case Some(data) =>
            request.getQueryString("qr-code") match {
              case Some("true") => auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get,
                "AddNinoToGoogleWalletFromQRCode", configDecorator.appName))
              case _ => auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get,
                "AddNinoToGoogleWallet", configDecorator.appName))
            }
            Redirect(data)
          case _ => NotFound
        }
      }(loginContinueUrl)
    }
  }

  def getGooglePassQrCode(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction).async {
    implicit request => {
      authorisedAsFMNUser { _ =>
        findMyNinoServiceConnector.getGooglePassQrCode(passId).map {
          case Some(data) =>
            auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get, "DisplayGoogleQRCode", configDecorator.appName))
            Ok(data)
          case _ => NotFound
        }
      }(loginContinueUrl)
    }
  }
}
