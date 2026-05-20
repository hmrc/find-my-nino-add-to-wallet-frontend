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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.FandFConnector
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import play.api.{Configuration, Environment}
import services.{IdentityVerificationFrontendService, IdentityVerificationResponse, PrecondFailed, Success, TechnicalIssue}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import views.html.identity.{SuccessView, TechnicalIssuesView};

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject() (
  val identityVerificationFrontendService: IdentityVerificationFrontendService,
  authConnector: AuthConnector,
  fandFConnector: FandFConnector,
  successView: SuccessView,
  technicalIssuesView: TechnicalIssuesView
)(implicit
  config: Configuration,
  env: Environment,
  ec: ExecutionContext,
  cc: MessagesControllerComponents,
  frontendAppConfig: FrontendAppConfig
) extends FMNBaseController(authConnector, fandFConnector)
    with I18nSupport {

  def uplift(redirectUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async {
    Future.successful(
      Redirect(redirectUrl.getOrElse(RedirectUrl(routes.StoreMyNinoController.onPageLoad.url)).get(OnlyRelative).url)
    )
  }

  def showUpliftJourneyOutcome(continueUrl: Option[RedirectUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      val journeyId =
        List(request.getQueryString("token"), request.getQueryString("journeyId")).flatten.headOption

      val retryUrl = routes.ApplicationController.uplift(continueUrl).url

      journeyId match {
        case Some(jid) =>
          identityVerificationFrontendService
            .getIVJourneyStatus(jid)
            .fold(
              error => {
                logErrorMessage(
                  s"Call to IdentityVerificationFrontendService failed for journeyId: $jid. ${error.message}"
                )
                InternalServerError(technicalIssuesView(retryUrl))
              },
              response => handleIVResponse(jid, response, continueUrl, retryUrl)
            )

        case None =>
          logErrorMessage("journeyId missing or incorrect")
          Future.successful(FailedDependency(technicalIssuesView(retryUrl)))
      }
    }

  private def handleIVResponse(
    journeyId: String,
    response: IdentityVerificationResponse,
    continueUrl: Option[RedirectUrl],
    retryUrl: String
  )(implicit request: Request[_]): Result = response match {
    case Success =>
      Ok(
        successView(
          continueUrl
            .getOrElse(RedirectUrl(routes.StoreMyNinoController.onPageLoad.url))
            .get(OnlyRelative)
            .url
        )
      )

    case TechnicalIssue =>
      logger.warn(s"TechnicalIssue response from IdentityVerificationFrontendService for journeyId: $journeyId")
      InternalServerError(technicalIssuesView(retryUrl))

    case PrecondFailed =>
      logger.error(
        s"PreconditionFailed response from IdentityVerificationFrontendService for journeyId: $journeyId. " +
          "This outcome should not occur because the user should not have been able to enter the service. " +
          "Investigate the identity verification journey for this journeyId."
      )
      InternalServerError(technicalIssuesView(retryUrl))

    case other =>
      logErrorMessage(
        s"Unexpected IV outcome '$other' for journeyId: $journeyId. " +
          "This outcome is no longer handled by this service and is expected to be resolved within identity verification."
      )
      FailedDependency(technicalIssuesView(retryUrl))
  }

  private def logErrorMessage(reason: String): Unit =
    logger.warn(s"Unable to confirm user identity: $reason")
}
