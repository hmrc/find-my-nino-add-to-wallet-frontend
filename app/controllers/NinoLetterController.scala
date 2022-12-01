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

import controllers.actions._
import models.PersonDetails
import org.apache.fop.apps.{FOUserAgent, Fop, FopFactory}
import org.apache.xmlgraphics.util.MimeConstants
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.ApacheFOPHelpers
import views.html.print._

import java.io.{ByteArrayOutputStream, File}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.{Result, Transformer, TransformerFactory}

class NinoLetterController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      identify: IdentifierAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: PrintNationalInsuranceNumberView
                                    ) extends FrontendBaseController with I18nSupport {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request => {
      Ok(view(PersonDetails.personDetails, LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")), true, PersonDetails.personDetails.person.nino.get))
    }
  }

  def saveNationalInsuranceNumberAsPdf(): Action[AnyContent] = Action {

    val xmlSrc = ApacheFOPHelpers.xmlData(
      PersonDetails.personDetails.person.title.get,
      PersonDetails.personDetails.person.firstName.get,
      PersonDetails.personDetails.person.lastName.get,
      PersonDetails.personDetails.person.nino.get,
      PersonDetails.personDetails.address.get.line1.get,
      PersonDetails.personDetails.address.get.line2.get,
      PersonDetails.personDetails.address.get.line3.get
      )

    val out: ByteArrayOutputStream = new ByteArrayOutputStream()
    val pdf = generateNinoPDF(xmlSrc, ApacheFOPHelpers.xslData, out)

    Ok(pdf).as(MimeConstants.MIME_PDF)
      .withHeaders(
        CONTENT_TYPE -> "application/x-download",
        CONTENT_DISPOSITION -> "attachment; filename=national-insurance-letter.pdf")

  }

  private def generateNinoPDF(xmlSrc: StreamSource, xslData: StreamSource, outStream: ByteArrayOutputStream): Array[Byte] = {

    try {
      val fopFactory: FopFactory = FopFactory.newInstance(new File("./app/assets").toURI())
      val foUserAgent: FOUserAgent = fopFactory.newFOUserAgent()
      foUserAgent.setAccessibility(true)

      val fop: Fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, outStream)

      val factory: TransformerFactory = TransformerFactory.newInstance()
      val transformer: Transformer = factory.newTransformer(xslData)
      transformer.setParameter("versionParam", "2.0")

      // Resulting SAX events (the generated FO) must be piped through to FOP
      val res: Result = new SAXResult(fop.getDefaultHandler())

      // Start XSLT transformation and FOP processing
      transformer.transform(xmlSrc, res)
      outStream.toByteArray()
    } catch {
      case e: Exception => outStream.toByteArray
    } finally {
      outStream.close()
    }
  }


}
