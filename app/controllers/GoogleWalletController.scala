/*
 * Copyright 2025 HM Revenue & Customs
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
import connectors.{FandFConnector, GoogleWalletConnector}
import controllers.actions.CheckChildRecordAction
import models.individualDetails.IndividualDetailsDataCache
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import play.api.{Configuration, Environment}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.AuditUtils
import views.html.{GoogleWalletView, PassIdNotFoundView, QRCodeNotFoundView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GoogleWalletController @Inject() (
  override val messagesApi: MessagesApi,
  authConnector: AuthConnector,
  fandFConnector: FandFConnector,
  view: GoogleWalletView,
  googleWalletConnector: GoogleWalletConnector,
  checkChildRecordAction: CheckChildRecordAction,
  auditService: AuditService,
  passIdNotFoundView: PassIdNotFoundView,
  qrCodeNotFoundView: QRCodeNotFoundView
)(implicit
  config: Configuration,
  env: Environment,
  ec: ExecutionContext,
  cc: MessagesControllerComponents,
  frontendAppConfig: FrontendAppConfig
) extends FMNBaseController(authConnector, fandFConnector)
    with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen checkChildRecordAction) async { userRequestNew =>
    (frontendAppConfig.googleWalletEnabled, userRequestNew.trustedHelper) match {
      case (_, Some(_)) => Future.successful(Redirect(controllers.routes.StoreMyNinoController.onPageLoad))
      case (true, None) =>
        implicit val hc: HeaderCarrier  =
          HeaderCarrierConverter.fromRequestAndSession(userRequestNew.request, userRequestNew.request.session)
        implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)

        auditGoogleWallet("ViewWalletPage", userRequestNew.individualDetails, hc)
        val nino: String  = userRequestNew.nino.getOrElse(throw new IllegalArgumentException("No nino found")).nino
        val ninoFormatted = nino.grouped(2).mkString(" ")
        val fullName      = userRequestNew.individualDetails.individualDetailsData.fullName
        googleWalletConnector
          .createGooglePass(fullName, ninoFormatted)
          .fold(
            error => InternalServerError(s"Failed to create Google Pass: ${error.message}"),
            pId => Ok(view(pId.value, isMobileDisplay(userRequestNew.request))(userRequestNew.request, messages))
          )

      case (false, _) => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
    }
  }

  private def isMobileDisplay(request: Request[AnyContent]): Boolean = {
    // Display wallet options differently on mobile to pc
    val strUserAgent = request.headers
      .get("http_user_agent")
      .getOrElse(
        request.headers
          .get("User-Agent")
          .getOrElse("")
      )

    config
      .get[String]("mobileDeviceDetectionRegexStr")
      .r
      .findFirstMatchIn(strUserAgent) match {
      case Some(_) => true
      case None    => false
    }
  }

  def getGooglePass(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen checkChildRecordAction) async {
    userRequestNew =>
      implicit val hc: HeaderCarrier  =
        HeaderCarrierConverter.fromRequestAndSession(userRequestNew.request, userRequestNew.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)

      googleWalletConnector
        .getGooglePassUrl(passId)
        .fold(
          error => InternalServerError(s"Failed to get Google Pass: ${error.message}"),
          {
            case Some(data) =>
              val eventType = userRequestNew.request.getQueryString("qr-code") match {
                case Some("true") => "AddNinoToWalletFromQRCode"
                case _            => "AddNinoToWallet"
              }
              auditGoogleWallet(eventType, userRequestNew.individualDetails, hc)
              Redirect(data)

            case None =>
              NotFound(passIdNotFoundView()(userRequestNew.request, messages, ec))
          }
        )
  }

  def getGooglePassQrCode(passId: String): Action[AnyContent] =
    (authorisedAsFMNUser andThen checkChildRecordAction) async { userRequestNew =>
      implicit val hc: HeaderCarrier  =
        HeaderCarrierConverter.fromRequestAndSession(userRequestNew.request, userRequestNew.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)

      googleWalletConnector
        .getGooglePassQrCode(passId)
        .fold(
          error => InternalServerError(s"Failed to get Google Pass QR Code: ${error.message}"),
          {
            case Some(data) =>
              auditGoogleWallet("DisplayQRCode", userRequestNew.individualDetails, hc)
              Ok(data)

            case None =>
              NotFound(qrCodeNotFoundView()(userRequestNew.request, messages, ec))
          }
        )
    }

  def auditGoogleWallet(
    eventType: String,
    individualDetailsDataCache: IndividualDetailsDataCache,
    hc: HeaderCarrier
  ): Unit =
    auditService.audit(
      AuditUtils.buildAuditEvent(
        individualDetailsDataCache,
        eventType,
        frontendAppConfig.appName,
        Some("Google")
      )(hc)
    )(hc)
}
