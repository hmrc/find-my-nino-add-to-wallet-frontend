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
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Configuration, Environment}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import views.html.identity._

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject() (
  val identityVerificationFrontendService: IdentityVerificationFrontendService,
  authConnector: AuthConnector,
  successView: SuccessView,
  cannotConfirmIdentityView: CannotConfirmIdentityView,
  failedIvIncompleteView: FailedIvIncompleteView,
  lockedOutView: LockedOutView,
  timeOutView: TimeOutView,
  technicalIssuesView: TechnicalIssuesView
)(implicit
  config: Configuration,
  env: Environment,
  ec: ExecutionContext,
  cc: MessagesControllerComponents,
  frontendAppConfig: FrontendAppConfig
) extends FMNBaseController(authConnector)
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
                logErrorMessage(s"Call to IdentityVerificationFrontendService failed: ${error.message}")
                InternalServerError(technicalIssuesView(retryUrl))
              },
              response => handleIVResponse(response, continueUrl, retryUrl)
            )

        case None =>
          logErrorMessage("journeyId missing or incorrect")
          Future.successful(FailedDependency(technicalIssuesView(retryUrl)))
      }
    }

  private def handleIVResponse(
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

    case InsufficientEvidence | UserAborted | FailedMatching | PrecondFailed =>
      logErrorMessage(response.toString)
      Unauthorized(cannotConfirmIdentityView(retryUrl))

    case Incomplete =>
      logErrorMessage(Incomplete.toString)
      Unauthorized(failedIvIncompleteView(retryUrl))

    case LockedOut =>
      logErrorMessage(LockedOut.toString)
      Unauthorized(lockedOutView(allowContinue = false))

    case Timeout =>
      logErrorMessage(Timeout.toString)
      Unauthorized(timeOutView(retryUrl))

    case TechnicalIssue =>
      logErrorMessage("TechnicalIssue response from IdentityVerificationFrontendService")
      FailedDependency(technicalIssuesView(retryUrl))

    case _ =>
      logErrorMessage("unknown status from IdentityVerificationFrontendService")
      FailedDependency(technicalIssuesView(retryUrl))
  }

  private def logErrorMessage(reason: String): Unit =
    logger.warn(s"Unable to confirm user identity: $reason")
}
