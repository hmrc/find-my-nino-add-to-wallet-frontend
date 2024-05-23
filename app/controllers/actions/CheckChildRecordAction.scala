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
import controllers.auth.requests._
import models.individualDetails.IndividualDetailsDataCache
import models.nps.CRNUpliftRequest
import play.api.http.Status._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.mvc._
import services.{IndividualDetailsService, NPSService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.RedirectToPostalFormView

import scala.concurrent.{ExecutionContext, Future}

class CheckChildRecordAction @Inject()(
                                        npsService: NPSService,
                                        individualDetailsService: IndividualDetailsService,
                                        cc: ControllerComponents,
                                        val messagesApi: MessagesApi,
                                        redirectView: RedirectToPostalFormView,
                                        frontendAppConfig: FrontendAppConfig
                                      )(implicit ec: ExecutionContext)
  extends ActionRefiner[AuthContext, UserRequestNew]
    with ActionFunction[AuthContext, UserRequestNew]
    with I18nSupport {

  override protected def refine[A](authContext: AuthContext[A]): Future[Either[Result, UserRequestNew[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
    val identifier: String = authContext.nino.nino

    val sessionId: String = hc.sessionId.map(_.value).getOrElse(
      throw new IllegalArgumentException("Session is required")
    )

    individualDetailsService.getIdDataFromCache(identifier, sessionId).flatMap {
      case Right(individualDetails) =>
        if (isCrn(individualDetails)) {
          fullNino(authContext, individualDetails)
        } else {
          actionCrnUpgrade(identifier, authContext, individualDetails, sessionId)
        }
      case Left(_) => Future.successful(throw new NotFoundException("Individual details not found"))
    }
  }

  private def isCrn(individualDetails: IndividualDetailsDataCache): Boolean =
    individualDetails.individualDetailsData.get.crnIndicator.toLowerCase.equals("false")

  private def fullNino[A](authContext: AuthContext[A], individualDetails: IndividualDetailsDataCache): Future[Right[Nothing, UserRequestNew[A]]] =
    Future.successful(
      Right(
        UserRequestNew(
          Some(Nino(individualDetails.getNino)),
          authContext.confidenceLevel,
          individualDetails,
          authContext.allEnrolments,
          authContext.request
        )
      )
    )

  private def actionCrnUpgrade[A](identifier: String, authContext: AuthContext[A], individualDetails: IndividualDetailsDataCache, sessionId: String)
                                 (implicit hc:HeaderCarrier): Future[Either[Result, UserRequestNew[A]]] = {
    val request: CRNUpliftRequest = buildCrnUpliftRequest(individualDetails)
    npsService.upliftCRN(identifier, request).map {
      case Right(_) => handleCrnUpliftSuccess(authContext, individualDetails, sessionId)
      case Left(status) => handleError(status, authContext, frontendAppConfig)
    }
  }

  private def handleCrnUpliftSuccess[A](authContext: AuthContext[A], individualDetails: IndividualDetailsDataCache, sessionId: String)
                                       (implicit hc: HeaderCarrier): Right[Nothing, UserRequestNew[A]] = {
    invalidateCache(individualDetails)
    validateCrnUplift(individualDetails.getNino, sessionId)
    Right(
      UserRequestNew(
        Some(Nino(individualDetails.getNino)),
        authContext.confidenceLevel,
        individualDetails,
        authContext.allEnrolments,
        authContext.request
      )
    )
  }

  private def buildCrnUpliftRequest(individualDetails: IndividualDetailsDataCache) =
    new CRNUpliftRequest(
      individualDetails.individualDetailsData.get.firstForename,
      individualDetails.individualDetailsData.get.surname,
      individualDetails.individualDetailsData.get.dateOfBirth.toString
    )

  private def validateCrnUplift(nino: String, sessionId: String)
                               (implicit hc: HeaderCarrier): Future[Boolean] =
    individualDetailsService.getIdDataFromCache(nino, sessionId).flatMap {
      case Right(individualDetails) =>
        if (individualDetails.individualDetailsData.get.crnIndicator.equals("true")) {
          throw new RuntimeException("CRN indicator still set to true after successful CRN uplift")
        } else {
          Future.successful(true)
        }
    }

  private def invalidateCache(individualDetails: IndividualDetailsDataCache): Future[Unit] =
    individualDetailsService.deleteIdDataFromCache(individualDetails.getNino).map(
      deleted => if (!deleted) {
        throw new RuntimeException("Could not invalidate the cache after CRN uplift")
      }
    )

  private def handleError[A](status: Int, authContext: AuthContext[A], frontendAppConfig: FrontendAppConfig): Left[Result, Nothing] = {
    implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
    status match
    {
      case BAD_REQUEST           => Left(InternalServerError(redirectView()(authContext.request, frontendAppConfig, messages)))
      case UNPROCESSABLE_ENTITY  => Left(InternalServerError(redirectView()(authContext.request, frontendAppConfig, messages)))
      case _                     => throw new InternalServerException("Something went wrong")
    }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}