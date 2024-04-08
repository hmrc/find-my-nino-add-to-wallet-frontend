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
import connectors.{AppleWalletConnector, CitizenDetailsConnector}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import services.{AuditService, IndividualDetailsService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.AuditUtils
import views.html.identity.TechnicalIssuesView
import views.html.{AppleWalletView, PassIdNotFoundView, QRCodeNotFoundView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AppleWalletController @Inject()(val citizenDetailsConnector: CitizenDetailsConnector,
                                      appleWalletConnector: AppleWalletConnector,
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      auditService: AuditService,
                                      individualDetailsService: IndividualDetailsService,
                                      view: AppleWalletView,
                                      passIdNotFoundView: PassIdNotFoundView,
                                      qrCodeNotFoundView: QRCodeNotFoundView,
                                      technicalIssuesView: TechnicalIssuesView
                                     )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad
  private val passFileName = "National-Insurance-number-card.pkpass"

  def onPageLoad: Action[AnyContent] = authorisedAsFMNUser async {
    authContext => {

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)

      //auditApple("ViewWalletPage", hc)

      individualDetailsService.getIdDataFromCache(authContext.nino.nino).flatMap {
        case Right(individualDetailsDataCache) =>
          val formattedNino = individualDetailsDataCache.getNino.grouped(2).mkString(" ")
          val fullName = individualDetailsDataCache.getFullName
          for{
            pId: Some[String] <- appleWalletConnector.createApplePass(fullName, formattedNino)
          } yield Ok(view(pId.value, isMobileDisplay(authContext.request))(authContext.request, messages))
        case Left("Individual details not found in cache") => Future.successful(InternalServerError(
          technicalIssuesView("Failed to get individual details from cache")(authContext.request, frontendAppConfig, messages)))
      }
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

  def getPassCard(passId: String): Action[AnyContent] = authorisedAsFMNUser async {
    authContext => {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
      appleWalletConnector.getApplePass(passId).map {
        case Some(data) =>
          val eventType = authContext.request.getQueryString("qr-code") match {
            case Some("true") => "AddNinoToWalletFromQRCode"
            case _ => "AddNinoToWallet"
          }
          //auditApple(eventType, hc)
          Ok(data).withHeaders("Content-Disposition" -> s"attachment; filename=$passFileName")
        case _ =>
          NotFound(passIdNotFoundView()(authContext.request, messages, ec))
      }
    }
  }



  def getQrCode(passId: String): Action[AnyContent] = authorisedAsFMNUser async {

    authContext => {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
      appleWalletConnector.getAppleQrCode(passId).map {
        case Some(data) =>
          //auditApple("DisplayQRCode", hc)
          Ok(data).withHeaders("Content-Disposition" -> s"attachment; filename=$passFileName")
        case _ => NotFound(qrCodeNotFoundView()(authContext.request, messages, ec))
      }
    }
  }

  private def auditApple(eventType:String, hc:HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(
      null,
      eventType,
      frontendAppConfig.appName,
      Some("Apple")
    )(hc))(hc)
  }

}
