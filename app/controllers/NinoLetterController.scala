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
import controllers.actions.CheckChildRecordAction
import controllers.auth.requests.UserRequestNew
import models.individualDetails.IndividualDetailsDataCache
import org.apache.xmlgraphics.util.MimeConstants
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.{AuditService, FopService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import util.{AuditUtils, XSLScalaBridge, XmlFoToPDF}
import views.html.print.PrintNationalInsuranceNumberView
import views.xml.ApplicationPdf

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NinoLetterController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      auditService: AuditService,
                                      view: PrintNationalInsuranceNumberView,
                                      checkChildRecordAction: CheckChildRecordAction,
                                      xmlFoToPDF: XmlFoToPDF,
                                      fopService: FopService,
                                      pdfTemplate: ApplicationPdf,
                                    )(implicit config: Configuration,
                                      env: Environment,
                                      ec: ExecutionContext,
                                      cc: MessagesControllerComponents,
                                      frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = (authorisedAsFMNUser andThen checkChildRecordAction) async {
    implicit userRequestNew => {
      implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)

      val nino: String = userRequestNew.nino.getOrElse(throw new IllegalArgumentException("No nino found")).nino
      val ninoFormatted = nino.grouped(2).mkString(" ")

      auditNinoLetter("ViewNinoLetter", userRequestNew.individualDetails, hc)
      Future.successful(Ok(view(userRequestNew.individualDetails, LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")),
        true, ninoFormatted)(userRequestNew.request, messages)))
    }
  }

  def saveNationalInsuranceNumberAsPdf: Action[AnyContent] = (authorisedAsFMNUser andThen checkChildRecordAction) async {
    implicit userRequestNew => {
      //implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)
      val filename = messagesApi.preferred(userRequestNew.request).messages("label.your_national_insurance_number_letter")

      auditNinoLetter("DownloadNinoLetter", userRequestNew.individualDetails, hc)
      //val pdf = createPDF(userRequestNew.individualDetails, messages)
      createPDF2(userRequestNew.individualDetails, userRequestNew).flatMap(pdf =>
        Future.successful(Ok(pdf).as(MimeConstants.MIME_PDF)
          .withHeaders(CONTENT_TYPE -> "application/x-download", CONTENT_DISPOSITION -> s"attachment; filename=${filename.replaceAll(" ", "-")}.pdf"))
      )
    }
  }

//  private def createPDF(individualDetailsDataCache: IndividualDetailsDataCache, messages: Messages): Array[Byte] = {
//    xmlFoToPDF.createPDF(
//      individualDetailsDataCache,
//      LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")),
//      messages
//    )
//  }

  private def createPDF2(individualDetailsDataCache: IndividualDetailsDataCache, userRequestNew: UserRequestNew[AnyContent]): Future[Array[Byte]] = {
    implicit val messages: Messages = cc.messagesApi.preferred(userRequestNew.request)
    val date: String                = LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY"))

    fopService.render(pdfTemplate(individualDetailsDataCache, date, XSLScalaBridge(messages).getLang()).body)
  }

  private def auditNinoLetter(eventType: String, individualDetailsDataCache: IndividualDetailsDataCache, hc: HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(individualDetailsDataCache, eventType, frontendAppConfig.appName, None)(hc))(hc)
  }
}