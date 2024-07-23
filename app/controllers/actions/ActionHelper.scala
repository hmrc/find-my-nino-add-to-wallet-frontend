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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.auth.AuthContext
import controllers.auth.requests.UserRequest
import handlers.ErrorHandler
import models.individualDetails.IndividualDetailsDataCache
import models.nps.CRNUpliftRequest
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, UNPROCESSABLE_ENTITY}
import play.api.i18n.Messages
import play.api.mvc.Results.{FailedDependency, Ok}
import play.api.mvc.{ControllerComponents, Result}
import services.{IndividualDetailsService, NPSService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import views.html.RedirectToPostalFormView
import views.html.identity.TechnicalIssuesNoRetryView

import scala.concurrent.{ExecutionContext, Future}

class ActionHelper @Inject()(individualDetailsService: IndividualDetailsService,
                             cc: ControllerComponents,
                             technicalIssuesNoRetryView: TechnicalIssuesNoRetryView,
                             errorHandler: ErrorHandler,
                             postalFormView: RedirectToPostalFormView,
                             frontendAppConfig: FrontendAppConfig,
                             npsService: NPSService) {
  
  def checkForCrn[A](identifier: String,
                     sessionId: String,
                     authContext: AuthContext[A],
                     messages: Messages)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Result, UserRequest[A]]] = {
    individualDetailsService.getIdDataFromCache(identifier, sessionId).flatMap {
      case Right(individualDetails) =>
        if (!isFullNino(individualDetails)) {
          if (frontendAppConfig.crnUpliftEnabled) {
            val request: CRNUpliftRequest = buildCrnUpliftRequest(individualDetails)

            for {
              upliftResult <- npsService.upliftCRN(identifier, request)
              preFlightChecks <- preFlightChecks(upliftResult.isRight, individualDetails, sessionId)
            } yield (upliftResult, preFlightChecks) match {
              case (Left(status), _) => handleErrorCrnUplift(status, authContext, frontendAppConfig)
              case (Right(_), true) =>
                Right(
                  UserRequest(
                    Some(Nino(individualDetails.getNino)),
                    authContext.confidenceLevel,
                    individualDetails,
                    authContext.allEnrolments,
                    authContext.request
                  )
                )
              case (Right(_), false) => Left(throw new InternalServerException("Failed to verify CRN uplift"))
              case _ => Left(throw new InternalServerException("Failed to uplift CRN"))
            }
          } else {
            Future.successful(Left(Ok(postalFormView()(authContext.request, frontendAppConfig, messages))))
          }
        } else {
          if (Nino.isValid(individualDetails.getNino)) {
            Future.successful(
              Right(
                UserRequest(
                  Some(Nino(individualDetails.getNino)),
                  authContext.confidenceLevel,
                  individualDetails,
                  authContext.allEnrolments,
                  authContext.request
                )
              )
            )
          } else {
            Future.successful(Left(Ok(postalFormView()(authContext.request, frontendAppConfig, messages))))
          }
        }
      case Left(response) => handleErrorIndividualDetails(response, authContext, frontendAppConfig)
    }
  }

  private def preFlightChecks(upliftSuccess: Boolean, individualDetails: IndividualDetailsDataCache, sessionId: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    if (upliftSuccess) {
      for {
        cacheInvalidated <- individualDetailsService.deleteIdDataFromCache(individualDetails.getNino)
        crnIndicatorUpdated <- validateCrnUplift(individualDetails.getNino, sessionId)
      } yield (cacheInvalidated, crnIndicatorUpdated) match {
        case (true, true) => true
        case _ => false
      }
    } else {
      Future.successful(true)
    }
  }

  private def isFullNino(individualDetails: IndividualDetailsDataCache): Boolean =
    individualDetails.individualDetailsData.get.crnIndicator.toLowerCase.equals("false")

  private def validateCrnUplift(nino: String, sessionId: String)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    individualDetailsService.getIdDataFromCache(nino, sessionId).flatMap {
      case Right(individualDetails) => Future.successful(isFullNino(individualDetails))
      case _ => Future.successful(throw new NotFoundException("Individual details not found"))
    }

  private def buildCrnUpliftRequest(individualDetails: IndividualDetailsDataCache): CRNUpliftRequest =
    new CRNUpliftRequest(
      individualDetails.individualDetailsData.get.firstForename,
      individualDetails.individualDetailsData.get.surname,
      individualDetails.individualDetailsData.get.dateOfBirth.toString
    )

  private def handleErrorIndividualDetails[A](response: Int,
                                              authContext: AuthContext[A],
                                              frontendAppConfig: FrontendAppConfig): Future[Left[Result, Nothing]] = {
    implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
    response match {
      case UNPROCESSABLE_ENTITY =>
        Future.successful(Left(Ok(technicalIssuesNoRetryView()(authContext.request, frontendAppConfig, messages))))
      case _ =>
        Future.successful(Left(FailedDependency(
          errorHandler.standardErrorTemplate(
            Messages("global.error.InternalServerError500.title"),
            Messages("global.error.InternalServerError500.heading"),
            Messages("global.error.InternalServerError500.message")
          )(authContext.request)
        )))
    }
  }

  private def handleErrorCrnUplift[A](status: Int,
                                      authContext: AuthContext[A],
                                      frontendAppConfig: FrontendAppConfig): Left[Result, Nothing] = {
    implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
    status match {
      case BAD_REQUEST => Left(Ok(postalFormView()(authContext.request, frontendAppConfig, messages)))
      case UNPROCESSABLE_ENTITY => Left(Ok(postalFormView()(authContext.request, frontendAppConfig, messages)))
      case NOT_FOUND => Left(Ok(postalFormView()(authContext.request, frontendAppConfig, messages)))
      case _ => throw new InternalServerException("Failed to uplift CRN")
    }
  }

}
