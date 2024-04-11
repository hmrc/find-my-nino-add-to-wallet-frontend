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
import controllers.auth.requests.{UserRequest, UserRequestNew}
import models.{GovUkPassCreateResponse, PersonDetails}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request}
import play.api.{Configuration, Environment}
import services.{AuditService, IndividualDetailsService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.AuditUtils
import views.html.{ErrorTemplate, GovUKWalletView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GovUKWalletController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      view: GovUKWalletView,
                                      errorTemplate: ErrorTemplate,
                                      //getPersonDetailsAction: GetPersonDetailsFromAuthAction,
                                      individualDetailsService: IndividualDetailsService,
                                      govUKWalletSMNConnector: GovUKWalletSMNConnector,
                                      auditService: AuditService
                                    )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig)
  extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad



  def onPageLoad: Action[AnyContent] = authorisedAsFMNUser async {
    authContext =>
      if (frontendAppConfig.govukWalletEnabled) {
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
        implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)

        individualDetailsService.getIdDataFromCache(authContext.nino.nino).flatMap {
          case Right(individualDetailsDataCache) =>
            val formattedNino = individualDetailsDataCache.getNino.grouped(2).mkString(" ")
            val title = individualDetailsDataCache.getTitle
            val givenName = individualDetailsDataCache.getFirstForename
            val familyName = individualDetailsDataCache.getLastName
            for {
              pId: Some[GovUkPassCreateResponse] <- govUKWalletSMNConnector.createGovUKPass(title, givenName, familyName, formattedNino)

            } yield Ok(view(
              pId.value.url,
              pId.value.bytes,
              isMobileDisplay(authContext.request))(authContext.request, messages))
        }
      }
      else {
        Future(Redirect(routes.UnauthorisedController.onPageLoad))
      }
  }



  private def isMobileDisplay(request: Request[AnyContent]): Boolean = {
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
