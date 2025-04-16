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
import handlers.ErrorHandler
import models.individualDetails.IndividualDetailsDataCache
import models.nps.CRNUpliftRequest
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, UNPROCESSABLE_ENTITY}
import play.api.i18n.Messages
import play.api.mvc.Results.{FailedDependency, InternalServerError, Ok}
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
  errorHandler: ErrorHandler,
  postalFormView: RedirectToPostalFormView,
  frontendAppConfig: FrontendAppConfig,
  npsService: NPSService
) {

  def checkForCrn[A](identifier: String, sessionId: String, authContext: AuthContext[A], messages: Messages)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Result, UserRequest[A]]] =
    individualDetailsService.getIdDataFromCache(identifier, sessionId).flatMap {
      case Right(individualDetails) =>
        if (!isFullNino(individualDetails)) {
          if (frontendAppConfig.crnUpliftEnabled) {
            val request: CRNUpliftRequest = buildCrnUpliftRequest(individualDetails)

            (for {
              _ <- npsService.upliftCRN(identifier, request)
              _ <- EitherT[Future, UpstreamErrorResponse, Boolean](
                     individualDetailsService
                       .deleteIdDataFromCache(individualDetails.individualDetailsData.nino)
                       .map(Right(_))
                   )
            } yield UserRequest(
              Some(Nino(individualDetails.individualDetailsData.nino)),
              authContext.confidenceLevel,
              individualDetails,
              authContext.allEnrolments,
              authContext.request,
              authContext.trustedHelper
            )).leftMap {
              case upstreamErrorResponse if upstreamErrorResponse.statusCode == BAD_REQUEST          =>
                Ok(postalFormView()(authContext.request, frontendAppConfig, messages))
              case upstreamErrorResponse if upstreamErrorResponse.statusCode == UNPROCESSABLE_ENTITY =>
                Ok(postalFormView()(authContext.request, frontendAppConfig, messages))
              case upstreamErrorResponse if upstreamErrorResponse.statusCode == NOT_FOUND            =>
                Ok(postalFormView()(authContext.request, frontendAppConfig, messages))
              case _                                                                                 => InternalServerError("Failed to verify CRN uplift")
            }.value
          } else {
            Future.successful(Left(Ok(postalFormView()(authContext.request, frontendAppConfig, messages))))
          }
        } else {
          validateNino(individualDetails, authContext, messages)
        }
      case Left(response)           => handleErrorIndividualDetails(response, authContext, frontendAppConfig)
    }

  private def validateNino[A](
    individualDetails: IndividualDetailsDataCache,
    authContext: AuthContext[A],
    messages: Messages
  ): Future[Either[Result, UserRequest[A]]] =
    if (Nino.isValid(individualDetails.individualDetailsData.nino)) {
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
    } else {
      Future.successful(Left(Ok(postalFormView()(authContext.request, frontendAppConfig, messages))))
    }

  private def isFullNino(individualDetails: IndividualDetailsDataCache): Boolean =
    individualDetails.individualDetailsData.crnIndicator.toLowerCase.equals("false")

  private def buildCrnUpliftRequest(individualDetails: IndividualDetailsDataCache): CRNUpliftRequest =
    new CRNUpliftRequest(
      individualDetails.individualDetailsData.firstForename,
      individualDetails.individualDetailsData.surname,
      individualDetails.individualDetailsData.dateOfBirth.toString
    )

  private def handleErrorIndividualDetails[A](
    response: Int,
    authContext: AuthContext[A],
    frontendAppConfig: FrontendAppConfig
  )(implicit ec: ExecutionContext): Future[Left[Result, Nothing]] = {
    implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
    response match {
      case UNPROCESSABLE_ENTITY =>
        Future.successful(Left(Ok(technicalIssuesNoRetryView()(authContext.request, frontendAppConfig, messages))))
      case _                    =>
        errorHandler
          .standardErrorTemplate(
            Messages("global.error.InternalServerError500.title"),
            Messages("global.error.InternalServerError500.heading"),
            Messages("global.error.InternalServerError500.message")
          )(authContext.request)
          .map(error => Left(FailedDependency(error)))
    }
  }
}
