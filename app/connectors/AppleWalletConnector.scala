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

import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.http.Status.*
import play.api.libs.json.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

case class ApplePassDetails(fullName: String, nino: String)

class AppleWalletConnector @Inject()(frontendAppConfig: FrontendAppConfig, http: HttpClientV2) {

  private val headers: Seq[(String, String)] = Seq("Content-Type" -> "application/json")
  implicit val writes: Writes[ApplePassDetails] = Json.writes[ApplePassDetails]

  def createApplePass(fullName: String, nino: String)
                     (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Some[String]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/create-apple-pass"
    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    val details = ApplePassDetails(fullName, nino)

    http.post(url"$url").withBody(Json.toJson(details)).execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Some(response.body)
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

  def getApplePass(passId: String)
                  (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[Array[Byte]]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-pass-card?passId=$passId"
    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.get(url"$url").execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Some(Base64.getDecoder.decode(response.body))
          case NOT_FOUND => None
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

  def getAppleQrCode(passId: String)
               (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[Array[Byte]]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-qr-code?passId=$passId"
    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.get(url"$url").execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Some(Base64.getDecoder.decode(response.body))
          case NOT_FOUND => None
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }
}