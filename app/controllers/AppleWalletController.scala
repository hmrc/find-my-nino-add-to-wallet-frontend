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
import connectors.AppleWalletConnector
import controllers.actions.CheckChildRecordAction
import models.individualDetails.IndividualDetailsDataCache
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.AuditUtils
import views.html.{AppleWalletView, PassIdNotFoundView, QRCodeNotFoundView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AppleWalletController @Inject()(val appleWalletConnector: AppleWalletConnector,
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      auditService: AuditService,
                                      view: AppleWalletView,
                                      passIdNotFoundView: PassIdNotFoundView,
                                      qrCodeNotFoundView: QRCodeNotFoundView,
                                      checkChildRecordAction: CheckChildRecordAction
                                     )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad
  private val passFileName = "National-Insurance-number-card.pkpass"

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen checkChildRecordAction) async {
    implicit userRequestNew => {
      (frontendAppConfig.appleWalletEnabled, userRequestNew.trustedHelper) match {
        case (_, Some(_)) => Future.successful(Redirect(controllers.routes.StoreMyNinoController.onPageLoad))
        case (true, None) =>
          implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(userRequestNew.request, userRequestNew.request.session)
          implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)

          auditApple("ViewWalletPage", userRequestNew.individualDetails, hc)
          val nino: String = userRequestNew.nino.getOrElse(throw new IllegalArgumentException("No nino found")).nino
          val ninoFormatted = nino.grouped(2).mkString(" ")
          val fullName = userRequestNew.individualDetails.individualDetailsData.fullName

          appleWalletConnector.createApplePass(fullName, ninoFormatted)
            .value
            .map{
              case Right(pId) => Ok(view(pId.value, isMobileDisplay(userRequestNew.request))(userRequestNew.request, messages))
              case Left(error) => InternalServerError(s"Failed to create Apple Pass: ${error.message}")
            }
        case (false, _) =>Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
      }
    }
  }

  def getPassCard(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen checkChildRecordAction) async {
    implicit userRequestNew => {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(userRequestNew.request, userRequestNew.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)

      getApplePass(passId, userRequestNew.individualDetails, userRequestNew.request, hc, messages)
    }
  }

  def getQrCode(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen checkChildRecordAction) async {

    userRequestNew => {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(userRequestNew.request, userRequestNew.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)

      getAppleQRCode(passId, userRequestNew.individualDetails, userRequestNew.request, hc, messages)
    }
  }


  private def isMobileDisplay(request: Request[AnyContent]): Boolean = {
    // Display wallet options differently on mobile to pc
    val strUserAgent = request.headers
      .get("http_user_agent")
      .getOrElse(request.headers.get("User-Agent")
        .getOrElse(""))

    config.get[String]("mobileDeviceDetectionRegexStr").r
      .findFirstMatchIn(strUserAgent) match {
      case Some(_) => true
      case None => false
    }
  }

  private def getApplePass(passId: String, individualDetailsDataCache: IndividualDetailsDataCache,
                           request: Request[AnyContent], hc:HeaderCarrier, messages: Messages): Future[Result] = {
    appleWalletConnector.getApplePass(passId)(ec, hc).value.map {
      case Right(Some(data)) =>
        val eventType = request.getQueryString("qr-code") match {
          case Some("true") => "AddNinoToWalletFromQRCode"
          case _ => "AddNinoToWallet"
        }
        auditApple(eventType, individualDetailsDataCache, hc)
        Ok(data).withHeaders("Content-Disposition" -> s"attachment; filename=$passFileName")

      case Right(None) =>
        NotFound(passIdNotFoundView()(request, messages, ec))

      case Left(error) => InternalServerError(s"Failed to get Apple Pass: ${error.message}")
    }
  }

  private def getAppleQRCode(passId: String, individualDetailsDataCache: IndividualDetailsDataCache,
                             request: Request[AnyContent], hc:HeaderCarrier, messages: Messages): Future[Result] = {
    appleWalletConnector.getAppleQrCode(passId)(ec, hc).value.map {
      case Right(Some(data)) =>
        auditApple("DisplayQRCode", individualDetailsDataCache, hc)
        Ok(data).withHeaders("Content-Disposition" -> s"attachment; filename=$passFileName")

      case Right(None) =>
        NotFound(qrCodeNotFoundView()(request, messages, ec))

      case Left(error) => InternalServerError(s"Failed to get Apple QR Code: ${error.message}")
    }
  }

  private def auditApple(eventType:String, individualDataCache:IndividualDetailsDataCache, hc:HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(
      individualDataCache,
      eventType,
      frontendAppConfig.appName,
      Some("Apple")
    )(hc))(hc)
  }
}