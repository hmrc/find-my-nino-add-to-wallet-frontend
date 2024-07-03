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

package services

import models.individualDetails.IndividualDetailsDataCache
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.fop.apps.{FOUserAgent, FopConfParser, FopFactory}
import org.apache.fop.events.model.EventSeverity
import org.apache.fop.events.{Event, EventFormatter, EventListener}
import org.apache.xmlgraphics.util.MimeConstants
import play.api.Logging
import play.api.i18n.Messages
import util.{BaseResourceStreamResolver, StylesheetResourceStreamResolver, XSLScalaBridge}

import java.io.{ByteArrayInputStream, File}
import javax.inject.{Inject, Singleton}
import javax.xml.transform.{ErrorListener, Transformer, TransformerException, TransformerFactory}
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using
import scala.xml.PrettyPrinter

@Singleton
class FopService @Inject()(
  resourceStreamResolver: BaseResourceStreamResolver,
  stylesheetResourceStreamResolver: StylesheetResourceStreamResolver
)(implicit ec: ExecutionContext) extends Logging {

  def createPDF(individualDetailsDataCache: IndividualDetailsDataCache, date: String, messages: Messages): Future[Array[Byte]] = Future {
    Using.resource(new ByteArrayOutputStream()) { out =>

      val fopConfigFilePath = "conf/pdf/fop.xconf"
      val niLetterXSLFilePath = "/pdf/niLetterXSL.xsl"

      val xconf = new File(fopConfigFilePath)
      val parser = new FopConfParser(xconf) //parsing configuration
      val builder = parser.getFopFactoryBuilder //building the factory with the user options
      val fopFactory = builder.build()

      // Turn on accessibility features
      val userAgent = fopFactory.newFOUserAgent()
      userAgent.setAccessibility(true)

      val fop = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, out)
      setupFOPEventHandling(userAgent)

      val xslStream: StreamSource = resourceStreamResolver.resolvePath(niLetterXSLFilePath)
      val transformerFactory = TransformerFactory.newInstance()
      transformerFactory.setURIResolver(stylesheetResourceStreamResolver)
      val transformer = transformerFactory.newTransformer(xslStream)

      setupTransformerEventHandling(transformer)
      transformer.setParameter("translator", XSLScalaBridge(messages))

      val source = new StreamSource(
        new ByteArrayInputStream(getXMLSource(individualDetailsDataCache, date))
      )

      val result = new SAXResult(fop.getDefaultHandler)

      transformer.transform(source, result)

      out.toByteArray
    }
  }

  def getXMLSource(individualDetailsDataCache: IndividualDetailsDataCache, date: String): Array[Byte] = {
    val addressElem =
      <address>
        {individualDetailsDataCache.getAddressLines.map { line =>
        <address-line>
          {line}
        </address-line>
      }}
      </address>
        <postcode>
          {individualDetailsDataCache.getPostCode.getOrElse("").toUpperCase}
        </postcode>
    val xmlElem = <root>
      <initials-name>
        {individualDetailsDataCache.getInitialsName}
      </initials-name>
      <full-name>
        {individualDetailsDataCache.getFullName}
      </full-name>{addressElem}<nino>
        {individualDetailsDataCache.getNino.grouped(2).mkString(" ")}
      </nino>
      <date>
        {date}
      </date>
    </root>
    val maxWidth = 80
    val tabSize = 2
    val p = new PrettyPrinter(maxWidth, tabSize)
    val xmlElemString = p.format(xmlElem)
    xmlElemString.getBytes()
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
}
