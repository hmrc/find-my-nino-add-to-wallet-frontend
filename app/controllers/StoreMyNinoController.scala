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
import connectors.{AppleWalletConnector, CitizenDetailsConnector, GoogleWalletConnector}
import controllers.auth.requests.{UserRequest, UserRequestNew}
import models.individualDetails.IndividualDetailsDataCache
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request}
import play.api.{Configuration, Environment}
import services.{AuditService, IndividualDetailsService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.AuditUtils
import views.html.StoreMyNinoView
import views.html.identity.TechnicalIssuesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StoreMyNinoController @Inject()(
                                       val citizenDetailsConnector: CitizenDetailsConnector,
                                       val appleWalletConnector: AppleWalletConnector,
                                       val googleWalletConnector: GoogleWalletConnector,
                                       authConnector: AuthConnector,
                                       auditService: AuditService,
                                       technicalIssuesView: TechnicalIssuesView,
                                       individualDetailsService: IndividualDetailsService,
                                       override val messagesApi: MessagesApi,
                                       getIndividualDetailsAction: GetIndividualDetailsAction,
                                       view: StoreMyNinoView
                                     )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {


  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = authorisedAsFMNUser andThen getIndividualDetailsAction async {
    implicit request => {
      //auditService.audit(AuditUtils.buildAuditEvent(request.personDetails, "ViewNinoLanding", frontendAppConfig.appName, None))
      val fullName: String = request.individualDetails.getFullName
      val nino: String = request.nino.map(_.formatted).getOrElse("")
      val googleIdf = googleWalletConnector.createGooglePass(fullName, nino)
      val appleIdf = appleWalletConnector.createApplePass(fullName, nino)

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(request)

      individualDetailsService.getIdDataFromCache(nino).flatMap {
        case Right(individualDetailsDataCache) =>
          //auditService.audit(AuditUtils.buildAuditEvent(request.personDetails, "ViewNinoLanding", frontendAppConfig.appName, None))
          auditSMNLandingPage("ViewNinoLanding", individualDetailsDataCache, hc)
          googleIdf.flatMap { googleId =>
            appleIdf.map { appleId =>
              Ok(view(appleId.value, googleId.value, nino, isMobileDisplay(request)))
            }
          }
        case Left("Individual details not found in cache") => Future.successful(InternalServerError(
          technicalIssuesView("Failed to get individual details from cache")(request, frontendAppConfig, messages)))
      }
    }
  }

  private def isMobileDisplay(request: UserRequestNew[AnyContent]): Boolean = {
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
    AuditUtils.buildAuditEvent(individualDetailsDataCache, event, frontendAppConfig.appName, None)(hc)
  }
}
