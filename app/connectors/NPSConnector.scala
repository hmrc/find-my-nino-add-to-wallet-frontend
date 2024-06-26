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

package connectors

import config.FrontendAppConfig
import models.nps.CRNUpliftRequest
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NPSConnector@Inject() (httpClientV2: HttpClientV2, appConfig: FrontendAppConfig) {

  def upliftCRN(nino: String, body: CRNUpliftRequest
                   )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val url = s"${appConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/adult-registration/$nino"

    httpClientV2
      .put(new URL(url))
      .withBody(body)
      .execute[HttpResponse]
      .flatMap{ response =>
        Future.successful(response)
      }
  }

}