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
import org.apache.fop.apps.FopFactory
import org.apache.xmlgraphics.util.MimeConstants
import play.api.i18n.Messages
import util.XSLScalaBridge

import java.io.ByteArrayInputStream
import javax.inject.{Inject, Singleton}
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using
import scala.xml.PrettyPrinter

@Singleton
class FopService @Inject()(
  fopFactory: FopFactory
)(implicit ec: ExecutionContext) {

  def createPDF(individualDetailsDataCache: IndividualDetailsDataCache, date: String, messages: Messages): Future[Array[Byte]] = Future {
    Using.resource(new ByteArrayOutputStream()) { out =>
      // Turn on accessibility features
      val userAgent = fopFactory.newFOUserAgent()
      userAgent.setAccessibility(true)

      val fop = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, out)

      val transformerFactory = TransformerFactory.newInstance()
      val transformer = transformerFactory.newTransformer()

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


    val xmlElem = <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:fox="http://xmlgraphics.apache.org/fop/extensions">
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
    </fo:root>
    val maxWidth = 80
    val tabSize = 2
    val p = new PrettyPrinter(maxWidth, tabSize)
    val xmlElemString = p.format(xmlElem)
    xmlElemString.getBytes()
  }
}
