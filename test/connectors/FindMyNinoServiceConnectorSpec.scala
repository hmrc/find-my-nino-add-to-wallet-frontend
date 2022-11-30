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

package connectors

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class FindMyNinoServiceConnectorSpec extends SpecBase with MockitoSugar{


  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.global //TODO: remove lazy keyword when Caching spec is done.

  val mockFMNConnector = mock[FindMyNinoServiceConnector]

  "Methods tests" - {
    "getApplePass" in {

      mockFMNConnector.getApplePass("Testing").isInstanceOf[Future[Option[Array[Byte]]]]
    }

    /*"getQrCode" in {

    }

    "createApplePass" in {

    }

    "writes" in {

    }*/
  }
}
