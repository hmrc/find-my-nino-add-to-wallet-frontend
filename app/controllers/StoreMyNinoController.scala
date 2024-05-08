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
import connectors.{AppleWalletConnector, GoogleWalletConnector}
import controllers.auth.requests.UserRequestNew
import models.individualDetails.IndividualDetailsDataCache
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import services.{AuditService, IndividualDetailsService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.AuditUtils
import views.html.StoreMyNinoView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StoreMyNinoController @Inject()(
                                       val appleWalletConnector: AppleWalletConnector,
                                       val googleWalletConnector: GoogleWalletConnector,
                                       authConnector: AuthConnector,
                                       auditService: AuditService,
                                       individualDetailsService: IndividualDetailsService,
                                       override val messagesApi: MessagesApi,
                                       view: StoreMyNinoView
                                     )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {


  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = authorisedAsFMNUser async {
    implicit authContext => {

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)

      val nino: String = authContext.nino.nino
      val ninoFormatted: String = nino.grouped(2).mkString(" ")
      val sessionId: String = hc.sessionId.get.value

      individualDetailsService.getIdDataFromCache(nino, sessionId).flatMap {
        case Right(individualDetailsDataCache) =>
          auditSMNLandingPage("ViewNinoLanding", individualDetailsDataCache, hc)
          val fullName = individualDetailsDataCache.getFullName
          val googleIdf = googleWalletConnector.createGooglePass(fullName, ninoFormatted)
          val appleIdf = appleWalletConnector.createApplePass(fullName, ninoFormatted)
          googleIdf.flatMap { googleId =>
            appleIdf.map { appleId =>
              Ok(view(appleId.value, googleId.value, ninoFormatted, isMobileDisplay(authContext.request))(authContext.request, messages))
            }
          }
        case Left("Individual details not found in cache") => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad(None)))
      }
    }
  }

  private def isMobileDisplay(request: Request[AnyContent]): Boolean = {
    val strUserAgent = request.headers.get("http_user_agent")
      .getOrElse(request.headers.get("User-Agent")
        .getOrElse(""))

    config.get[String]("mobileDeviceDetectionRegexStr").r
      .findFirstMatchIn(strUserAgent) match {
      case Some(_) => true
      case None => false
    }
  }

  private def auditSMNLandingPage(event: String, individualDetailsDataCache: IndividualDetailsDataCache,
                                  hc: HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(individualDetailsDataCache, event, frontendAppConfig.appName, None)(hc))(hc)
  }
}
