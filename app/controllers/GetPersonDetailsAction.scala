/*
 * Copyright 2023 HM Revenue & Customs
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
import config.ConfigDecorator
import connectors.{CitizenDetailsConnector, PersonDetailsHiddenResponse, PersonDetailsNotFoundResponse, PersonDetailsSuccessResponse}
import controllers.auth.AuthContext
import controllers.auth.requests.UserRequest
import models.{PersonDetails, UserName}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.{InternalServerError, Locked, NotFound, Ok}
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.RedirectToPostalFormView

import scala.concurrent.{ExecutionContext, Future}

class GetPersonDetailsAction @Inject()(
                                        citizenDetailsConnector: CitizenDetailsConnector,
                                        cc: ControllerComponents,
                                        val messagesApi: MessagesApi,
                                        redirectView: RedirectToPostalFormView
                                      )(implicit configDecorator: ConfigDecorator, ec: ExecutionContext)
  extends ActionRefiner[AuthContext, UserRequest]
    with ActionFunction[AuthContext, UserRequest]
    with I18nSupport {

  override protected def refine[A](authContext: AuthContext[A]): Future[Either[Result, UserRequest[A]]] = {

    getPersonDetails(authContext).map {
      case Left(error) => Left(error)
      case Right(personDetails) =>
        Right(
          UserRequest(
            Some(Nino(authContext.nino.nino)),
            Some(UserName(authContext.name)),
            authContext.confidenceLevel,
            personDetails,
            authContext.allEnrolments,
            authContext.request
          )
        )
    }
  }

  private def getPersonDetails(authContext: AuthContext[_]): Future[Either[Result, PersonDetails]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)

    implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
        citizenDetailsConnector.personDetails(authContext.nino.nino).map {
          case PersonDetailsSuccessResponse(pd) => Right(pd)
          case PersonDetailsNotFoundResponse =>
            Left(InternalServerError(redirectView()(authContext.request, configDecorator, messages)))
          case PersonDetailsHiddenResponse =>
            Left(Ok(redirectView()(authContext.request, configDecorator, messages)))
          case _ => Left(InternalServerError(redirectView()(authContext.request, configDecorator, messages)))
    }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}


