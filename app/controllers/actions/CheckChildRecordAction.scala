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
import controllers.auth.AuthContext
import controllers.auth.requests._
import models.nps.CRNUpliftRequest
import play.api.http.Status._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{IndividualDetailsDataCache, NPSService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CheckChildRecordAction @Inject()(
                                        npsService: NPSService,
                                        cc: ControllerComponents,
                                        val messagesApi: MessagesApi,
                                      )(implicit hc: HeaderCarrier, ec: ExecutionContext)
  extends ActionRefiner[AuthContext, UserRequest1694]
    with ActionFunction[AuthContext, UserRequest1694]
    with I18nSupport {

  override protected def refine[A](authContext: AuthContext[A]): Future[Either[Result, UserRequest1694[A]]] = {

    val nino: String = authContext.nino.nino
    val sessionId: String = hc.sessionId.map(_.value).getOrElse(throw new IllegalArgumentException("Session is required"))

    npsService.getIdDataFromCache(nino, sessionId).flatMap {
      case Right(individualDetails) =>
        if (individualDetails.crnIndicator) {
          val request: CRNUpliftRequest = buildCrnUpliftRequest(individualDetails)
          npsService.upliftCRN("foo", request).map {
            case Right(_) =>
              Right(
                UserRequest1694(
                  Some(Nino(nino)),
                  authContext.confidenceLevel,
                  individualDetails,
                  authContext.allEnrolments,
                  authContext.request
                )
              )
            case Left(error) => handleError(error.responseCode)
          }
        } else {
          Future.successful(
            Right(
              UserRequest1694(
                Some(Nino(nino)),
                authContext.confidenceLevel,
                individualDetails,
                authContext.allEnrolments,
                authContext.request
              )
            )
          )
        }
      case Left(error) => handleError(error.responseCode)
    }
  }

  private def buildCrnUpliftRequest(individualDetails: IndividualDetailsDataCache) =
    new CRNUpliftRequest(
      individualDetails.firstForename,
      individualDetails.surname,
      individualDetails.dateOfBirth
    )

  private def handleError(code: Int) =
    code match
    {
      case INTERNAL_SERVER_ERROR => ??? // Standard error redirect
      case BAD_REQUEST => ??? // Contact redirect
      case UNPROCESSABLE_ENTITY => ??? // Contact redirect
    }

  override protected def executionContext: ExecutionContext = cc.executionContext
}


