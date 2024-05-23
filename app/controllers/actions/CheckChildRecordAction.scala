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
        if (!isFullNino(individualDetails)) {
          val request: CRNUpliftRequest = buildCrnUpliftRequest(individualDetails)

          for {
            upliftResult <- npsService.upliftCRN(identifier, request)
            _            = if (upliftResult.isRight) verifyCrnUplift(individualDetails, sessionId)
          } yield upliftResult match {
            case Left(status) => handleError(status, authContext, frontendAppConfig)
            case Right(_) =>
              Right(
                UserRequestNew(
                  Some(Nino(identifier)),
                  authContext.confidenceLevel,
                  individualDetails,
                  authContext.allEnrolments,
                  authContext.request
                )
              )
            case _ => Left(throw new NotFoundException("Individual details not found"))
          }
        } else {
          Future.successful(
            Right(
              UserRequestNew(
                Some(Nino(identifier)),
                authContext.confidenceLevel,
                individualDetails,
                authContext.allEnrolments,
                authContext.request
              )
            )
          )
        }
      case Left(_) => Future.successful(throw new NotFoundException("Individual details not found"))
    }
  }

  private def verifyCrnUplift(individualDetails: IndividualDetailsDataCache, sessionId: String)
                    (implicit hc: HeaderCarrier): Future[Boolean] = {
    for {
      cacheInvalidated    <- individualDetailsService.deleteIdDataFromCache(individualDetails.getNino)
      crnIndicatorUpdated <- validateCrnUplift(individualDetails.getNino, sessionId)
    } yield (cacheInvalidated, crnIndicatorUpdated) match {
      case (true, true) => true
      case _            => throw new InternalServerException("Failed to verify the CRN uplift")
    }
  }

  private def isFullNino(individualDetails: IndividualDetailsDataCache): Boolean =
    individualDetails.individualDetailsData.get.crnIndicator.toLowerCase.equals("false")

  private def validateCrnUplift(nino: String, sessionId: String)
                               (implicit hc: HeaderCarrier): Future[Boolean] =
    individualDetailsService.getIdDataFromCache(nino, sessionId).flatMap {
      case Right(individualDetails) => Future.successful(isFullNino(individualDetails))
      case _                        => Future.successful(throw new NotFoundException("Individual details not found"))
    }

  private def buildCrnUpliftRequest(individualDetails: IndividualDetailsDataCache) =
    new CRNUpliftRequest(
      individualDetails.individualDetailsData.get.firstForename,
      individualDetails.individualDetailsData.get.surname,
      individualDetails.individualDetailsData.get.dateOfBirth.toString
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