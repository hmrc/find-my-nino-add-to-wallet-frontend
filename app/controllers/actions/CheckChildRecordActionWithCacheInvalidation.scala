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
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import services.IndividualDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class CheckChildRecordActionWithCacheInvalidation @Inject() (
  individualDetailsService: IndividualDetailsService,
  cc: ControllerComponents,
  val messagesApi: MessagesApi,
  actionHelper: ActionHelper
)(implicit ec: ExecutionContext)
    extends ActionRefiner[AuthContext, UserRequest]
    with ActionFunction[AuthContext, UserRequest]
    with I18nSupport {

  override protected def refine[A](authContext: AuthContext[A]): Future[Either[Result, UserRequest[A]]] = {
    implicit val hc: HeaderCarrier  =
      HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
    implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
    val identifier: String          = authContext.nino.nino

    val sessionId: String = hc.sessionId
      .map(_.value)
      .getOrElse(
        throw new IllegalArgumentException("Session is required")
      )

    individualDetailsService.deleteIdDataFromCache(identifier).flatMap {
      case true => actionHelper.checkForCrn(identifier, sessionId, authContext, messages).value
      case _    => throw new RuntimeException("Failed to invalidate individual details data cache")
    }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}
