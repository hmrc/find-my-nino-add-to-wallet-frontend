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
import controllers.auth.AuthContext
import models.individualDetails.IndividualDetailsDataCache
import org.apache.xmlgraphics.util.MimeConstants
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.{AuditService, IndividualDetailsService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.{AuditUtils, XmlFoToPDF}
import views.html.identity.TechnicalIssuesView
import views.html.print.PrintNationalInsuranceNumberView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NinoLetterController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      auditService: AuditService,
                                      view: PrintNationalInsuranceNumberView,
                                      technicalIssuesView: TechnicalIssuesView,
                                      individualDetailsService: IndividualDetailsService,
                                      xmlFoToPDF: XmlFoToPDF
                                    )(implicit config: Configuration,
                                      env: Environment,
                                      ec: ExecutionContext,
                                      cc: MessagesControllerComponents,
                                      frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad: Action[AnyContent] = authorisedAsFMNUser async {
    authContext => {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)

      individualDetailsService.getIdDataFromCache(authContext.nino.nino).flatMap {
        case Right(individualDetailsDataCache) =>
          auditNinoLetter("ViewNinoLetter", individualDetailsDataCache, hc)
          val formattedNino = individualDetailsDataCache.getNino.grouped(2).mkString(" ")
          Future.successful(Ok(view(
            individualDetailsDataCache,
            LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")),
            true,
            formattedNino)(authContext.request, messages)))
        case Left("Individual details not found in cache") => catchAllError(authContext, messages)
        case _ => catchAllError(authContext, messages)
      }
    }
  }

  private def catchAllError(authContext: AuthContext[AnyContent], messages: Messages)  =
    Future.successful(InternalServerError(
      technicalIssuesView("Failed to get individual details from cache")(authContext.request, frontendAppConfig, messages)))


  def saveNationalInsuranceNumberAsPdf: Action[AnyContent] = authorisedAsFMNUser async {
    authContext => {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(authContext.request, authContext.request.session)
      implicit val messages: Messages = cc.messagesApi.preferred(authContext.request)
      val filename = messagesApi.preferred(authContext.request).messages("label.your_national_insurance_number_letter")

      individualDetailsService.getIdDataFromCache(authContext.nino.nino).flatMap {
        case Right(individualDetailsDataCache) => {
          auditNinoLetter("DownloadNinoLetter", individualDetailsDataCache, hc)
          val pdf = createPDF(individualDetailsDataCache, messages)
          Future.successful(Ok(pdf).as(MimeConstants.MIME_PDF)
            .withHeaders(CONTENT_TYPE -> "application/x-download", CONTENT_DISPOSITION -> s"attachment; filename=${filename.replaceAll(" ", "-")}.pdf"))
        }
        case Left("Individual details not found in cache") => catchAllError(authContext, messages)
        case _ => catchAllError(authContext, messages)

      }
    }
  }

  private def createPDF(individualDetailsDataCache: IndividualDetailsDataCache, messages: Messages): Array[Byte] = {
    xmlFoToPDF.createPDF(
      individualDetailsDataCache,
      LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")),
      messages
    )
  }

  private def auditNinoLetter(eventType: String, individualDetailsDataCache: IndividualDetailsDataCache, hc: HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(individualDetailsDataCache, eventType, frontendAppConfig.appName, None)(hc))(hc)
  }
}