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

import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.fop.apps.{FOUserAgent, Fop, FopConfParser, FopFactory, FopFactoryBuilder}
import org.apache.xmlgraphics.util.MimeConstants

import java.io.{File, StringReader}
import javax.inject.{Inject, Singleton}
import javax.xml.transform.{Transformer, TransformerFactory}
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

@Singleton
class FopService @Inject()()(implicit ec: ExecutionContext) {

  def render(input: String): Future[Array[Byte]] = Future {

    Using.resource(new ByteArrayOutputStream()) { out =>

      val fopConfigFilePath: String  = "conf/pdf/fop.xconf"
      val xconf: File                = new File(fopConfigFilePath)
      val parser: FopConfParser      = new FopConfParser(xconf) //parsing configuration
      val builder: FopFactoryBuilder = parser.getFopFactoryBuilder //building the factory with the user options
      val fopFactory: FopFactory     = builder.build()

      // Turn on accessibility features
      val userAgent: FOUserAgent = fopFactory.newFOUserAgent()
      userAgent.setAccessibility(true)

      val fop: Fop                               = fopFactory.newFop(MimeConstants.MIME_PDF, userAgent, out)
      val transformerFactory: TransformerFactory = TransformerFactory.newInstance()
      val transformer: Transformer               = transformerFactory.newTransformer()
      val source: StreamSource                   = new StreamSource(new StringReader(input))
      val result: SAXResult                      = new SAXResult(fop.getDefaultHandler)

      transformer.transform(source, result)
      out.toByteArray
    }
  }
}
