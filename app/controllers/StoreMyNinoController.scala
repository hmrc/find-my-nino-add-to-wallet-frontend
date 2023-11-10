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

import config.{ConfigDecorator, FrontendAppConfig}
import connectors.{StoreMyNinoConnector, CitizenDetailsConnector}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import util.AuditUtils
import views.html.{ StoreMyNinoView, ErrorTemplate }

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StoreMyNinoController @Inject()(
                                       val citizenDetailsConnector: CitizenDetailsConnector,
                                       findMyNinoServiceConnector: StoreMyNinoConnector,
                                       authConnector: AuthConnector,
                                       auditService: AuditService,
                                       override val messagesApi: MessagesApi,
                                       getPersonDetailsAction: GetPersonDetailsAction,
                                       view: StoreMyNinoView,
                                       errorTemplate: ErrorTemplate
                                     )(implicit config: Configuration,
                                       configDecorator: ConfigDecorator,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents,
                                       frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {


  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  private val passFileName = "National-Insurance-number-card.pkpass"

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) async {
    implicit request => {
      request.personDetails match {
        case Some(pd) =>
          auditService.audit(AuditUtils.buildAuditEvent(Some(pd), "ViewNinoLanding", configDecorator.appName, None))
          Future(Ok(view(request.nino.map(_.formatted).getOrElse(""))))
        case None =>
          Future(NotFound(errorTemplate("Details not found", "Your details were not found.", "Your details were not found, please try again later.")))
      }
    }
  }
}


