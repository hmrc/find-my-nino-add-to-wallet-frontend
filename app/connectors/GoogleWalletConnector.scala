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
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._

import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

case class GooglePassDetails(fullName: String, nino: String)

class GoogleWalletConnector @Inject()(frontendAppConfig: FrontendAppConfig, http: HttpClient) {

  private val headers: Seq[(String, String)] = Seq("Content-Type" -> "application/json")
  implicit val writes: Writes[GooglePassDetails] = Json.writes[GooglePassDetails]

  def createGooglePass(fullName: String, nino: String)
                      (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Some[String]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/create-google-pass-with-credentials"
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    val details = GooglePassDetails(fullName, nino)

    http.POST[JsValue, HttpResponse](url, Json.toJson(details))(implicitly, implicitly, hc, implicitly)
      .map { response =>
        response.status match {
          case OK => Some(response.body)
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

  def getGooglePassUrl(passId: String)
                      (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[String]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-google-pass-url?passId=$passId"
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.GET[HttpResponse](url)(implicitly, hc, implicitly)
      .map { response =>
        response.status match {
          case OK => Some(response.body)
          case NOT_FOUND => None
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

  def getGooglePassQrCode(passId: String)
                         (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[Array[Byte]]] = {

    val url = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-google-qr-code?passId=$passId"
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.GET[HttpResponse](url)(implicitly, hc, implicitly)
      .map { response =>
        response.status match {
          case OK => Some(Base64.getDecoder.decode(response.body))
          case NOT_FOUND => None
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }
}