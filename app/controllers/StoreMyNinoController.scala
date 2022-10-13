/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.FindMyNinoServiceConnector
import controllers.actions._
import forms.StoreMyNinoProvider
import pages.StoreMyNinoPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.StoreMyNinoView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StoreMyNinoController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       findMyNinoServiceConnector: FindMyNinoServiceConnector,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       formProvider: StoreMyNinoProvider,
                                       view: StoreMyNinoView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => {
      val preparedForm = request.userAnswers.get(StoreMyNinoPage).fold(form)(form.fill)
      Ok(view(preparedForm))
    }
  }

  def getPassCard(passId: String): Action[AnyContent] = Action.async {
    implicit request => {
      findMyNinoServiceConnector.getApplePass(passId)
        .map {
          case Some(data) => Ok(data).withHeaders("Content-Disposition" -> "attachment; filename=NinoPass.pkpass")
          case _ => NotFound
        }
    }
  }

  def getQrCode(passId: String): Action[AnyContent] = Action.async {
    implicit request => {
      findMyNinoServiceConnector.getQrCode(passId)
        .map {
          case Some(data) => Ok(data).withHeaders("Content-Disposition" -> "attachment; filename=NinoPass.pkpass")
          case _ => NotFound
        }
    }
  }
}
