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
import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

case class ApplePassDetails(fullName: String, nino: String)

class AppleWalletConnector @Inject()(frontendAppConfig: FrontendAppConfig, httpClientV2: HttpClientV2,
                                     httpClientResponse: HttpClientResponse) {

  private val headers: Seq[(String, String)] = Seq("Content-Type" -> "application/json")
  implicit val writes: Writes[ApplePassDetails] = Json.writes[ApplePassDetails]

  def createApplePass(fullName: String, nino: String)
                     (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Some[String]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/create-apple-pass"
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    val details = ApplePassDetails(fullName, nino)

    httpClientResponse.read(
      httpClientV2
        .post(url"$url")(hc)
        .withBody(Json.toJson(details))
        .execute[Either[UpstreamErrorResponse, HttpResponse]](readEitherOf(readRaw), ec)
    ).map(response => Some(response.body))
  }

  def getApplePass(passId: String)
                  (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Option[Array[Byte]]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-pass-card?passId=$passId"

    fetchAppleData(url)
  }

  def getAppleQrCode(passId: String)
               (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Option[Array[Byte]]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-qr-code?passId=$passId"

    fetchAppleData(url)
  }

  private def fetchAppleData(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, UpstreamErrorResponse, Option[Array[Byte]]] = {
    val updatedHc = hc.withExtraHeaders(headers: _*)

    httpClientResponse.read(
      httpClientV2
        .get(url"$url")(updatedHc)
        .execute[Either[UpstreamErrorResponse, HttpResponse]](readEitherOf(readRaw), ec)
    ).map(response =>
      if (response.status == OK) Some(Base64.getDecoder.decode(response.body)) else None
    )
  }
}