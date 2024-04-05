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

package controllers

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.auth.AuthContext
import controllers.auth.requests.UserRequestNew
import models.UserName
import models.individualDetails.IndividualDetails
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.InternalServerError
import play.api.mvc._
import services.IndividualDetailsService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.RedirectToPostalFormView

import scala.concurrent.{ExecutionContext, Future}

class GetIndividualDetailsAction @Inject()(
                                        individualDetailsService: IndividualDetailsService,
                                        cc: ControllerComponents,
                                        val messagesApi: MessagesApi,
                                        redirectView: RedirectToPostalFormView
                                      )(implicit frontendAppConfig: FrontendAppConfig, ec: ExecutionContext)
  extends ActionRefiner[AuthContext, UserRequestNew]
    with ActionFunction[AuthContext, UserRequestNew]
    with I18nSupport {

  override protected def refine[A](authContext: AuthContext[A]): Future[Either[Result, UserRequestNew[A]]] = {

    getIndividualDetails(authContext).map {
      case Left(error) => Left(error)
      case Right(individualDetails) =>
        Right(
          UserRequestNew(
            Some(Nino(authContext.nino.nino)),
            Some(UserName(authContext.name)),
            authContext.confidenceLevel,
            individualDetails,
            authContext.allEnrolments,
            authContext.request
          )
        )
    }
  }

  private def getIndividualDetails(authContext: AuthContext[_]): Future[Either[Result, IndividualDetails]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
    implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)

    individualDetailsService.getIndividualDetails(authContext.nino.nino).map {
      case Right(individualDetails) =>
        individualDetailsService.createIndividualDetailsDataCache(authContext.request.session.data.getOrElse("sessionId", ""), individualDetails)
        Right(individualDetails)
      case Left(connectorError) => Left(InternalServerError(redirectView()(authContext.request, frontendAppConfig, messages)))
    }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}


