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

package controllers.actions

import cats.data.EitherT
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.auth.AuthContext
import controllers.auth.requests.UserRequest
import models.individualDetails.IndividualDetailsDataCache
import models.nps.CRNUpliftRequest
import play.api.i18n.Messages
import play.api.mvc.Results.{InternalServerError, Ok}
import play.api.mvc.{ControllerComponents, Result}
import services.{IndividualDetailsService, NPSService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import views.html.RedirectToPostalFormView
import views.html.identity.TechnicalIssuesNoRetryView

import scala.concurrent.{ExecutionContext, Future}

class ActionHelper @Inject() (
  individualDetailsService: IndividualDetailsService,
  cc: ControllerComponents,
  technicalIssuesNoRetryView: TechnicalIssuesNoRetryView,
  postalFormView: RedirectToPostalFormView,
  frontendAppConfig: FrontendAppConfig,
  npsService: NPSService
) {

  def checkForCrn[A](identifier: String, sessionId: String, authContext: AuthContext[A], messages: Messages)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Result, UserRequest[A]]] =
    individualDetailsService.getIdData(identifier, sessionId).value.flatMap {
      case Left(_) =>
        val messages: Messages = cc.messagesApi.preferred(authContext.request)
        Future.successful(
          Left(InternalServerError(technicalIssuesNoRetryView()(authContext.request, frontendAppConfig, messages)))
        )

      case Right(individualDetails) =>
        (isFullNino(individualDetails), frontendAppConfig.crnUpliftEnabled) match {
          case (true, _)     =>
            Future.successful(
              Right(
                UserRequest(
                  Some(Nino(individualDetails.individualDetailsData.nino)),
                  authContext.confidenceLevel,
                  individualDetails,
                  authContext.allEnrolments,
                  authContext.request,
                  authContext.trustedHelper
                )
              )
            )
          case (false, true) =>
            // Nino is not a full nino and uplift is enabled, Uplift nino with NPS
            val request: CRNUpliftRequest = buildCrnUpliftRequest(individualDetails)

            (for {
              _ <- npsService.upliftCRN(identifier, request)
              _ <- EitherT[Future, UpstreamErrorResponse, Boolean](
                     individualDetailsService
                       .deleteIdData(individualDetails.individualDetailsData.nino)
                       .map(Right(_))
                   )
            } yield UserRequest(
              Some(Nino(individualDetails.individualDetailsData.nino)),
              authContext.confidenceLevel,
              individualDetails,
              authContext.allEnrolments,
              authContext.request,
              authContext.trustedHelper
            )).leftMap { _ =>
              val messages: Messages = cc.messagesApi.preferred(authContext.request)
              InternalServerError(technicalIssuesNoRetryView()(authContext.request, frontendAppConfig, messages))
            }.value

          case _ =>
            // CRN uplift disabled, redirecting user to paper form
            Future.successful(Left(Ok(postalFormView()(authContext.request, frontendAppConfig, messages))))
        }
    }

  private def isFullNino(individualDetails: IndividualDetailsDataCache): Boolean =
    individualDetails.individualDetailsData.crnIndicator.toLowerCase.equals("false")

  private def buildCrnUpliftRequest(individualDetails: IndividualDetailsDataCache): CRNUpliftRequest =
    new CRNUpliftRequest(
      individualDetails.individualDetailsData.firstForename,
      individualDetails.individualDetailsData.surname,
      individualDetails.individualDetailsData.dateOfBirth.toString
    )
}
