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

// $COVERAGE-OFF$
package controllers

import com.google.inject.Inject
import controllers.auth.AuthContext
import controllers.auth.requests.UserRequest
import models.PersonDetails
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import scala.concurrent.{ExecutionContext, Future}
import models.Person
import play.api.i18n.MessagesApi
import play.api.i18n.I18nSupport

// NOT in use, since the deprecation of CitizenDetails usage
class GetPersonDetailsFromAuthAction @Inject()(cc: ControllerComponents,
                                        val messagesApi: MessagesApi
                                      )(ec: ExecutionContext)
  extends ActionRefiner[AuthContext, UserRequest]
    with ActionFunction[AuthContext, UserRequest]
    with I18nSupport {

  override protected def refine[A](authContext: AuthContext[A]): Future[Either[Result, UserRequest[A]]] = {
    Future.successful(Right(
      UserRequest(
        Some(Nino(authContext.nino.nino)),
        authContext.confidenceLevel,
        getPersonDetails(authContext),
        authContext.allEnrolments,
        authContext.request
      )
    ))
  }

  private def getPersonDetails(authContext: AuthContext[_]): PersonDetails = {
    PersonDetails(
      Person(
        None,
        None,
        None,
        None, None, None, None, None,
        Some(Nino(authContext.nino.nino))
      ),
      None,
      None
    )
  }

  override protected def executionContext: ExecutionContext = cc.executionContext
}
