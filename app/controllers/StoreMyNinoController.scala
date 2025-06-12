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
import connectors.{AppleWalletConnector, FandFConnector, GoogleWalletConnector}
import controllers.actions.CheckChildRecordActionWithCacheInvalidation
import models.individualDetails.IndividualDetailsDataCache
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.*
import play.api.{Configuration, Environment}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import util.AuditUtils
import views.html.StoreMyNinoView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StoreMyNinoController @Inject() (
  val appleWalletConnector: AppleWalletConnector,
  val googleWalletConnector: GoogleWalletConnector,
  authConnector: AuthConnector,
  fandFConnector: FandFConnector,
  auditService: AuditService,
  override val messagesApi: MessagesApi,
  view: StoreMyNinoView,
  checkChildRecordAction: CheckChildRecordActionWithCacheInvalidation
)(implicit
  config: Configuration,
  env: Environment,
  ec: ExecutionContext,
  cc: MessagesControllerComponents,
  frontendAppConfig: FrontendAppConfig
) extends FMNBaseController(authConnector, fandFConnector)
    with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen checkChildRecordAction) async {
    implicit userRequestNew =>
      implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)

      val nino: String          = userRequestNew.nino.getOrElse(throw new IllegalArgumentException("No nino found")).nino
      val ninoFormatted: String = nino.grouped(2).mkString(" ")

      auditSMNLandingPage("ViewNinoLanding", userRequestNew.individualDetails, hc)

      val fullName = userRequestNew.individualDetails.individualDetailsData.fullName

      val result = for {
        googleId <- googleWalletConnector.createGooglePass(fullName, ninoFormatted)
        appleId  <- appleWalletConnector.createApplePass(fullName, ninoFormatted)
      } yield Ok(
        view(
          appleId.value,
          googleId.value,
          ninoFormatted,
          isMobileDisplay(userRequestNew.request),
          userRequestNew.trustedHelper
        )(userRequestNew, messages)
      )

      result.value.map {
        case Right(result) => result
        case Left(error)   => InternalServerError(s"Error: ${error.message}")
      }
  }

  private def isMobileDisplay(request: Request[AnyContent]): Boolean = {
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

  private def auditSMNLandingPage(
    event: String,
    individualDetailsDataCache: IndividualDetailsDataCache,
    hc: HeaderCarrier
  ): Unit =
    auditService.audit(
      AuditUtils.buildAuditEvent(individualDetailsDataCache, event, frontendAppConfig.appName, None)(hc)
    )(hc)
}
