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
import models.nps.CRNUpliftRequest
import play.api.http.Status._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.mvc._
import services.{IndividualDetailsDataCache, NPSService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import views.html.RedirectToPostalFormView

import scala.concurrent.{ExecutionContext, Future}

class CheckChildRecordAction @Inject()(
                                        npsService: NPSService,
                                        cc: ControllerComponents,
                                        val messagesApi: MessagesApi,
                                        redirectView: RedirectToPostalFormView,
                                        frontendAppConfig: FrontendAppConfig
                                      )(implicit hc: HeaderCarrier, ec: ExecutionContext)
  extends ActionRefiner[AuthContext, UserRequest1694]
    with ActionFunction[AuthContext, UserRequest1694]
    with I18nSupport {

  override protected def refine[A](authContext: AuthContext[A]): Future[Either[Result, UserRequest1694[A]]] = {

    val identifier: String = authContext.nino.nino

    val sessionId: String = hc.sessionId.map(_.value).getOrElse(
      throw new IllegalArgumentException("Session is required")
    )

    npsService.getIdDataFromCache(identifier, sessionId).flatMap {
      case Right(individualDetails) =>
        if (individualDetails.crnIndicator) {
          val request: CRNUpliftRequest = buildCrnUpliftRequest(individualDetails)
          npsService.upliftCRN(identifier, request).map {
            case Right(_) =>
              Right(
                UserRequest1694(
                  Some(Nino(identifier)),
                  authContext.confidenceLevel,
                  individualDetails,
                  authContext.allEnrolments,
                  authContext.request
                )
              )
            case Left(error) => handleError(error.responseCode, authContext, frontendAppConfig)
          }
        } else {
          Future.successful(
            Right(
              UserRequest1694(
                Some(Nino(identifier)),
                authContext.confidenceLevel,
                individualDetails,
                authContext.allEnrolments,
                authContext.request
              )
            )
          )
        }
      case Left(error) => Future.successful(handleError(error.responseCode, authContext, frontendAppConfig))
    }
  }

  private def buildCrnUpliftRequest(individualDetails: IndividualDetailsDataCache) =
    new CRNUpliftRequest(
      individualDetails.firstForename,
      individualDetails.surname,
      individualDetails.dateOfBirth
    )

  private def handleError[A](code: Int, authContext: AuthContext[A], frontendAppConfig: FrontendAppConfig): Left[Result, Nothing] = {
    implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
    code match
    {
      case UNAUTHORIZED          => Left(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
      case BAD_REQUEST           => Left(InternalServerError(redirectView()(authContext.request, frontendAppConfig, messages)))
      case UNPROCESSABLE_ENTITY  => Left(InternalServerError(redirectView()(authContext.request, frontendAppConfig, messages)))
      case INTERNAL_SERVER_ERROR => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case _                     => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}


