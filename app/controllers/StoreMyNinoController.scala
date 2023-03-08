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
import connectors.{ApplePassConnector, CitizenDetailsConnector}
import models.PersonDetails
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import util.AuditUtils
import views.html.StoreMyNinoView

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class StoreMyNinoController @Inject()(
                                       val citizenDetailsConnector: CitizenDetailsConnector,
                                       findMyNinoServiceConnector: ApplePassConnector,
                                       authConnector: AuthConnector,
                                       auditService: AuditService,
                                       override val messagesApi: MessagesApi,
                                       getPersonDetailsAction: GetPersonDetailsAction,
                                       configDecorator: ConfigDecorator,
                                       view: StoreMyNinoView
                                     )(implicit config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {


  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) {
    implicit request => {
      val pd: PersonDetails = request.personDetails.get
      auditService.audit(AuditUtils.buildAuditEvent(pd,"ViewNinoLanding", configDecorator.appName))
      val pdId = Await.result(findMyNinoServiceConnector.createPersonDetailsRow(pd), 10 seconds).getOrElse("")
      val passId: String = Await.result(findMyNinoServiceConnector.createApplePass(pd.person.fullName, request.nino.get.nino), 10 seconds).getOrElse("")

      // Display wallet options differently on mobile to pc
      var displayForMobile: Boolean = false
      var strUserAgent = ""
      strUserAgent = request.headers.get("http_user_agent").getOrElse("")
      if (strUserAgent.isEmpty) {
        strUserAgent = request.headers.get("User-Agent").getOrElse("")
      }
      // Include any kind of mobile device except iPad, and also include Apple Watch
      val regexInclude = config.get[String]("mobileDeviceDetectionRegexStr").r
      regexInclude.findFirstMatchIn(strUserAgent) match {
        case Some(_) => displayForMobile = true
        case None => displayForMobile = false
      }

      Ok(view(passId, request.nino.get.formatted, pdId, displayForMobile))
    }
  }


  def getPassCard(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction).async {
    implicit request => {
      authorisedAsFMNUser { _ =>
        findMyNinoServiceConnector.getApplePass(passId).map {
          case Some(data) =>
            request.getQueryString("qr-code") match {
              case Some("true") => auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get,
                "AddNinoToWalletFromQRCode", configDecorator.appName))
              case _ => auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get,
                "AddNinoToWallet", configDecorator.appName))
            }
            Ok(data).withHeaders("Content-Disposition" -> "attachment; filename=NinoPass.pkpass")
          case _ => NotFound
        }
      }(loginContinueUrl)
    }
  }

  def getQrCode(passId: String): Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction).async {
    implicit request => {
      authorisedAsFMNUser { _ =>
        findMyNinoServiceConnector.getQrCode(passId).map {
          case Some(data) =>
            auditService.audit(AuditUtils.buildAuditEvent(request.personDetails.get,"DisplayQRCode",configDecorator.appName))
            Ok(data).withHeaders("Content-Disposition" -> "attachment; filename=NinoPass.pkpass")
          case _ => NotFound
        }
      }(loginContinueUrl)
    }
  }
}


