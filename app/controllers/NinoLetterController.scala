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

import config.FrontendAppConfig
import connectors.ApplePassConnector
import controllers.actions._
import models.PersonDetails
import org.apache.fop.apps.{FOUserAgent, Fop, FopFactory}
import org.apache.xmlgraphics.util.MimeConstants
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import util.{ApacheFOPHelpers, XmlFoToPDF}
import play.api.libs.json._
import views.html.print.PrintNationalInsuranceNumberView

import java.io.{ByteArrayOutputStream, File}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.{Result, Transformer, TransformerFactory}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class NinoLetterController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      authConnector: AuthConnector,
                                      applePassConnector: ApplePassConnector,
                                      view: PrintNationalInsuranceNumberView,
                                      xmlFoToPDF: XmlFoToPDF
                                    )(implicit config: Configuration,
                                      env: Environment,
                                      ec: ExecutionContext,
                                      cc: MessagesControllerComponents,
                                      frontendAppConfig: FrontendAppConfig) extends FMNBaseController(authConnector) with I18nSupport {


  implicit val loginContinueUrl: Call = routes.StoreMyNinoController.onPageLoad

  def onPageLoad(pdId: String): Action[AnyContent] = Action async {
    implicit request => {
      authorisedAsFMNUser { authContext =>
        for {
          pd <- applePassConnector.getPersonDetails(pdId)
        } yield {
          val personDetails = Json.fromJson[PersonDetails](Json.parse(Json.parse(pd.get).asInstanceOf[JsString].value)).get
          val personNino = personDetails.person.nino.get.nino
          Ok(view(
            personDetails,
            LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")),
            true,
            personNino, pdId))
        }
      }(routes.NinoLetterController.onPageLoad(pdId))

    }
  }

  def saveNationalInsuranceNumberAsPdf(pdId: String): Action[AnyContent] = Action async {

    implicit request => {
      authorisedAsFMNUser { authContext =>
        val pd = Await.result(applePassConnector.getPersonDetails(pdId), 10 seconds).getOrElse("xxx")
        val personDetails = Json.fromJson[PersonDetails](Json.parse(Json.parse(pd).asInstanceOf[JsString].value)).get

        val pdf = xmlFoToPDF.createPDF(personDetails.person.initialsName,
          personDetails.person.fullName,
          personDetails.person.nino.get.nino,
          personDetails.address.get.lines,
          personDetails.address.get.postcode.get,
          LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY"))
        )

        Future(Ok(pdf).as(MimeConstants.MIME_PDF)
          .withHeaders(
            CONTENT_TYPE -> "application/x-download",
            CONTENT_DISPOSITION -> "attachment; filename=national-insurance-letter.pdf"))
      }(routes.NinoLetterController.onPageLoad(pdId))
    }
  }
}
