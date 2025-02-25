/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.data.EitherT
import config.FrontendAppConfig
import models.nps.CRNUpliftRequest
import play.api.http.Status.UNPROCESSABLE_ENTITY
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NPSConnector@Inject() (
  httpClientV2: HttpClientV2,
  appConfig: FrontendAppConfig,
  httpClientResponse: HttpClientResponse)
{

  def upliftCRN(nino: String, body: CRNUpliftRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext):
  EitherT[Future, UpstreamErrorResponse, HttpResponse] = {

    val url = s"${appConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/adult-registration/$nino"

    httpClientResponse.read(
      httpClientV2
        .put(url"$url")
        .withBody(body)
        .execute[Either[UpstreamErrorResponse, HttpResponse]](readEitherOfWithUnProcessableEntity, implicitly)
    )
  }


  private def readEitherOfWithUnProcessableEntity[A: HttpReads]: HttpReads[Either[UpstreamErrorResponse, A]] =
    HttpReads.ask.flatMap {
      case (_, _, response) if response.status == UNPROCESSABLE_ENTITY => HttpReads[A].map(Right.apply)
      case _                                                => HttpReads[Either[UpstreamErrorResponse, A]]
    }
}