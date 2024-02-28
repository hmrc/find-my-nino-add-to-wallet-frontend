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

import config.FrontendAppConfig
import org.apache.xmlgraphics.util.MimeConstants
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import util.{AuditUtils, XmlFoToPDF}
import views.html.print.PrintNationalInsuranceNumberView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class NinoLetterController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      auditService: AuditService,
                                      view: PrintNationalInsuranceNumberView,
                                      getPersonDetailsAction: GetPersonDetailsAction,
                                      xmlFoToPDF: XmlFoToPDF
                                    )(implicit config: Configuration,
                                      env: Environment,
                                      ec: ExecutionContext,
                                      cc: MessagesControllerComponents,
                                      frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) {
    implicit request => {
        auditService.audit(AuditUtils.buildAuditEvent(request.personDetails, "ViewNinoLetter", frontendAppConfig.appName, None))
        Ok(view(
          request.personDetails,
          LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")),
          true,
          request.personDetails.person.nino.getOrElse(Nino("")).formatted))
      }
    }

    def saveNationalInsuranceNumberAsPdf: Action[AnyContent] = (authorisedAsFMNUser andThen getPersonDetailsAction) {
    implicit request => {
      auditService.audit(AuditUtils.buildAuditEvent(request.personDetails, "DownloadNinoLetter", frontendAppConfig.appName, None))
      val pdf = xmlFoToPDF.createPDF(request.personDetails,
        LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")),
        messagesApi.preferred(request)
      )

      val filename = messagesApi.preferred(request).messages("label.your_national_insurance_number_letter")

      Ok(pdf).as(MimeConstants.MIME_PDF)
        .withHeaders(
          CONTENT_TYPE -> "application/x-download",
          CONTENT_DISPOSITION -> s"attachment; filename=${filename.replaceAll(" ", "-")}.pdf")
      }
  }

}

