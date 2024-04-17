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

package util

import models.individualDetails.IndividualDetailsDataCache
import org.apache.fop.apps._
import org.apache.fop.events.model.EventSeverity
import org.apache.fop.events.{Event, EventFormatter, EventListener}
import org.apache.xmlgraphics.util.MimeConstants
import play.api.Logging
import play.api.i18n.Messages

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import javax.inject.{Inject, Singleton}
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.{ErrorListener, Transformer, TransformerException, TransformerFactory}
import scala.xml.PrettyPrinter

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

  def createPDF(individualDetailsDataCache: IndividualDetailsDataCache, date: String, messages: Messages): Array[Byte] = {
    val xmlStream: StreamSource = new StreamSource(
      new ByteArrayInputStream(getXMLSource(individualDetailsDataCache, date))
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

  def getXMLSource(individualDetailsDataCache: IndividualDetailsDataCache, date: String): Array[Byte] = {
    val addressElem =
          <address>
          {individualDetailsDataCache.getAddressLines.map { line =>
            <address-line>{line}</address-line>
          }}
          </address>
          <postcode>{individualDetailsDataCache.getPostCode.getOrElse("").toUpperCase}</postcode>


    val xmlElem = <root>
      <initials-name>{individualDetailsDataCache.getInitialsName}</initials-name>
      <full-name>{individualDetailsDataCache.getFullName}</full-name>
      {addressElem}
      <nino>{individualDetailsDataCache.getNino}</nino>
      <date>{date}</date>
    </root>
    val maxWidth = 80
    val tabSize = 2
    val p = new PrettyPrinter(maxWidth, tabSize)
    val xmlElemString = p.format(xmlElem)
    xmlElemString.getBytes()
  }
}


