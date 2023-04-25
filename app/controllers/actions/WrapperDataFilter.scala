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

package controllers.actions

import akka.stream.Materializer
import config.FrontendAppConfig
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import util.Keys

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WrapperDataFilter @Inject()(scaWrapperDataConnector: ScaWrapperDataConnector, appConfig: FrontendAppConfig)
                                 (implicit val executionContext: ExecutionContext, val mat: Materializer) extends Filter {

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    if (appConfig.SCAWrapperEnabled) {
      implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(rh, rh.session)
      implicit val head: RequestHeader = rh
      scaWrapperDataConnector.wrapperData()
        .map {resp => rh.addAttr(Keys.wrapperDataKey, resp)}
        .flatMap(f)
    } else {
      f(rh)
    }
  }
}