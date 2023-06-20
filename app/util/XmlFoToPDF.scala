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

package util

import models.PersonDetails
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import javax.inject.{Inject, Singleton}
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.{ErrorListener, Transformer, TransformerException, TransformerFactory}
import javax.xml.transform.stream.StreamSource
import org.apache.fop.apps._
import org.apache.xmlgraphics.util.MimeConstants
import org.apache.fop.events.{Event, EventFormatter, EventListener}
import org.apache.fop.events.model.EventSeverity
import play.api.Logging
import play.api.i18n.Messages
import play.twirl.api.utils.StringEscapeUtils
import uk.gov.hmrc.domain.Nino

@Singleton
class DefaultXmlFoToPDF @Inject()(val stylesheetResourceStreamResolver: StylesheetResourceStreamResolver,
                                  val resourceStreamResolver: BaseResourceStreamResolver,
                                  val fopURIResolver: FopURIResolver) extends XmlFoToPDF

trait XmlFoToPDF extends Logging{
  val resourceStreamResolver: BaseResourceStreamResolver
  val stylesheetResourceStreamResolver: StylesheetResourceStreamResolver
  val fopURIResolver: FopURIResolver

  private val fopConfigFilePath = "pdf/fop.xconf"
  private val niLetterXSLFilePath = "pdf/niLetterXSL.xsl"

  private def escapeHTMLEntitiesForPersonDetails(personDetails: PersonDetails): PersonDetails = {
    personDetails.copy(
      person = personDetails.person.copy(
        firstName = Some(StringEscapeUtils.escapeXml11(personDetails.person.firstName.getOrElse(""))),
        lastName = Some(StringEscapeUtils.escapeXml11(personDetails.person.lastName.getOrElse(""))),
        nino = Some(Nino(StringEscapeUtils.escapeXml11(personDetails.person.nino.getOrElse(Nino("")).nino)))
      ),
      address = personDetails.address.map(address => address.copy(
        line1 = Some(StringEscapeUtils.escapeXml11(address.line1.getOrElse(""))),
        line2 = Some(StringEscapeUtils.escapeXml11(address.line2.getOrElse(""))),
        line3 = Some(StringEscapeUtils.escapeXml11(address.line3.getOrElse(""))),
        line4 = Some(StringEscapeUtils.escapeXml11(address.line4.getOrElse(""))),
        line5 = Some(StringEscapeUtils.escapeXml11(address.line5.getOrElse(""))),
        postcode = Some(StringEscapeUtils.escapeXml11(address.postcode.getOrElse("")))
      )),
      correspondenceAddress = personDetails.correspondenceAddress.map(address => address.copy(
        line1 = Some(StringEscapeUtils.escapeXml11(address.line1.getOrElse(""))),
        line2 = Some(StringEscapeUtils.escapeXml11(address.line2.getOrElse(""))),
        line3 = Some(StringEscapeUtils.escapeXml11(address.line3.getOrElse(""))),
        line4 = Some(StringEscapeUtils.escapeXml11(address.line4.getOrElse(""))),
        line5 = Some(StringEscapeUtils.escapeXml11(address.line5.getOrElse(""))),
        postcode = Some(StringEscapeUtils.escapeXml11(address.postcode.getOrElse("")))
      ))
    )
  }



  def createPDF(personDetails: PersonDetails, date: String, messages: Messages): Array[Byte] = {
    //html escape personDetails to avoid xss  attack in pdf
    val escapedPersonDetails = escapeHTMLEntitiesForPersonDetails(personDetails)

    val xmlStream: StreamSource = new StreamSource(
      new ByteArrayInputStream(getXMLSource(escapedPersonDetails, date))
    )
    val pdfOutStream = new ByteArrayOutputStream()
    createTransformer(messages).transform(xmlStream, new SAXResult(fop(pdfOutStream).getDefaultHandler))

    pdfOutStream.write(pdfOutStream.toByteArray)
    pdfOutStream.toByteArray
  }

  private def createTransformer(messages: Messages): Transformer = {
    val xslStream: StreamSource = resourceStreamResolver.resolvePath(niLetterXSLFilePath)
    val transformerFactory: TransformerFactory = TransformerFactory.newInstance
    transformerFactory.setURIResolver(stylesheetResourceStreamResolver)

    val transformer: Transformer = transformerFactory.newTransformer(xslStream)
    setupTransformerEventHandling(transformer)

    transformer.setParameter("translator", XSLScalaBridge(messages))

    transformer
  }

  private def fop(pdfOutStream: ByteArrayOutputStream): Fop = {
    val restrictedIO: EnvironmentProfile = EnvironmentalProfileFactory
      .createRestrictedIO(new File(".").toURI, fopURIResolver)
    val confBuilder = new FopConfParser(resourceStreamResolver.resolvePath(fopConfigFilePath).getInputStream, restrictedIO).getFopFactoryBuilder
    val fopFactory: FopFactory = confBuilder.build
    val foUserAgent: FOUserAgent = fopFactory.newFOUserAgent

    setupFOPEventHandling(foUserAgent)

    fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, pdfOutStream)
  }

  private def setupFOPEventHandling(foUserAgent: FOUserAgent): Unit = {
    val eventListener = new EventListener {
      override def processEvent(event: Event): Unit = {
        val messages = EventFormatter.format(event)

        val optionErrorMsg = event.getSeverity match {
          case EventSeverity.INFO =>
            logger.info(messages)
            None
          case EventSeverity.WARN =>
            logger.debug(messages)
            None
          case EventSeverity.ERROR =>
            logger.error(messages)
            Some(messages)
          case EventSeverity.FATAL =>
            logger.error(messages)
            Some(messages)
          case _ => None
        }
        optionErrorMsg.foreach(errorMsg => throw new RuntimeException(errorMsg))
      }
    }
    foUserAgent.getEventBroadcaster.addEventListener(eventListener)
  }

  private def setupTransformerEventHandling(transformer: Transformer): Unit = {
    val errorListener = new ErrorListener {
      override def warning(exception: TransformerException): Unit =
        logger.debug(exception.getMessageAndLocation)

      override def error(exception: TransformerException): Unit = {
        logger.error(exception.getMessage, exception)
        throw exception
      }

      override def fatalError(exception: TransformerException): Unit = {
        logger.error(exception.getMessage, exception)
        throw exception
      }
    }
    transformer.setErrorListener(errorListener)
  }

  def getXMLSource(personDetails: PersonDetails, date: String): Array[Byte] = {
    val initialsNameXML = s"<initials-name>${personDetails.person.initialsName}</initials-name>"
    val fullNameXML = s"<full-name>${personDetails.person.fullName}</full-name>"
    var fullAddressXML = s""
    fullAddressXML = personDetails.correspondenceAddress.map { correspondenceAddress =>
      var xmlStr = s"<address>"
      xmlStr = xmlStr + correspondenceAddress.lines.map {line =>
        s"<address-line>${line}</address-line>"
      }
      xmlStr = xmlStr + s"</address>"
      xmlStr = xmlStr + s"<postcode>${correspondenceAddress.postcode.getOrElse("")}</postcode>"
      xmlStr
    }.getOrElse {
      personDetails.address.map { residentialAddress =>
        var xmlStr = s"<address>"
        xmlStr = xmlStr + residentialAddress.lines.map { line =>
          s"<address-line>${line}</address-line>"
        }
        xmlStr = xmlStr + s"</address>"
        xmlStr = xmlStr + s"<postcode>${residentialAddress.postcode.getOrElse("")}</postcode>"
        xmlStr
      }.getOrElse("")
    }
    val ninoXML = s"<nino>${personDetails.person.nino.get.formatted}</nino>"
    val dateXML = s"<date>${date}</date>"
    val xml = s"<root>" + initialsNameXML + fullNameXML + fullAddressXML + ninoXML + dateXML + s"</root>"
    xml.getBytes
  }

}


