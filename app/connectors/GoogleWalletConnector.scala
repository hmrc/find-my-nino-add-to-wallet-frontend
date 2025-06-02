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
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

case class GooglePassDetails(fullName: String, nino: String)

class GoogleWalletConnector @Inject() (
  frontendAppConfig: FrontendAppConfig,
  httpClientV2: HttpClientV2,
  httpClientResponse: HttpClientResponse
) extends Logging {

  private val headers: Seq[(String, String)]     = Seq("Content-Type" -> "application/json")
  implicit val writes: Writes[GooglePassDetails] = Json.writes[GooglePassDetails]

  def createGooglePass(fullName: String, nino: String)(implicit
    ec: ExecutionContext,
    headerCarrier: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Some[String]] = {
    val url                        =
      s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/create-google-pass-with-credentials"
    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)
    val details                    = Json.toJson(GooglePassDetails(fullName, nino))

    httpClientResponse
      .read(
        httpClientV2
          .post(url"$url")
          .withBody(details)
          .execute[Either[UpstreamErrorResponse, HttpResponse]](readEitherOf(readRaw), ec)
      )
      .transform {
        case Right(response) if response.status == OK =>
          logger.info("Google pass created successfully.")
          Right(Some(response.body))

        case Right(response) =>
          logger.info(s"Google pass creation failed with status: ${response.status}, body: ${response.body}")
          Left(UpstreamErrorResponse("", response.status))

        case Left(upstreamError) =>
          logger.error(s"Error creating google pass ${upstreamError.message}", upstreamError)
          Left(upstreamError)
      }
  }

  def getGooglePassUrl(
    passId: String
  )(implicit
    ec: ExecutionContext,
    headerCarrier: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Option[String]] = {

    val url                        = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-google-pass-url?passId=$passId"
    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    httpClientResponse
      .read(
        httpClientV2
          .get(url"$url")
          .execute[Either[UpstreamErrorResponse, HttpResponse]](readEitherOfWithNotFound)
      )
      .transform {
        case Right(response) if response.status == OK =>
          logger.info(
            s"Successfully retrieved Google Pass URL for passId: $passId. Response status: ${response.status}"
          )
          Right(Some(response.body))

        case Right(response) if response.status == NOT_FOUND =>
          logger.info(s"Google Pass URL not found for passId: $passId. Response status: ${response.status}")
          Right(None)

        case Right(response) =>
          logger.info(
            s"Failed to retrieve Google Pass URL for passId: $passId. Response status: ${response.status}, body: ${response.body}"
          )
          Left(UpstreamErrorResponse("", response.status))

        case Left(upstreamError) =>
          logger.error(
            s"Error retrieving Google Pass URL for passId: $passId. Upstream error: ${upstreamError.message}",
            upstreamError
          )
          Left(upstreamError)
      }
  }

  def getGooglePassQrCode(
    passId: String
  )(implicit
    ec: ExecutionContext,
    headerCarrier: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Option[Array[Byte]]] = {

    val url                        = s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-google-qr-code?passId=$passId"
    implicit val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    httpClientResponse
      .read(
        httpClientV2
          .get(url"$url")
          .execute[Either[UpstreamErrorResponse, HttpResponse]](readEitherOfWithNotFound)
      )
      .transform {
        case Right(response) if response.status == OK =>
          logger.info(
            s"Successfully retrieved Google Pass QR code for passId: $passId. Response status: ${response.status}"
          )
          Right(Some(Base64.getDecoder.decode(response.body)))

        case Right(response) if response.status == NOT_FOUND =>
          logger.info(s"Google Pass QR code not found for passId: $passId. Response status: ${response.status}")
          Right(None)

        case Right(response) =>
          logger.info(
            s"Failed to retrieve Google Pass QR code for passId: $passId. Response status: ${response.status}, body: ${response.body}"
          )
          Left(UpstreamErrorResponse("", response.status))

        case Left(upstreamError) =>
          logger.error(
            s"Error retrieving Google Pass QR code for passId: $passId. Upstream error: ${upstreamError.message}",
            upstreamError
          )
          Left(upstreamError)
      }
  }

  private def readEitherOfWithNotFound[A: HttpReads]: HttpReads[Either[UpstreamErrorResponse, A]] =
    HttpReads.ask.flatMap {
      case (_, _, response) if response.status == NOT_FOUND => HttpReads[A].map(Right.apply)
      case _                                                => HttpReads[Either[UpstreamErrorResponse, A]]
    }
}
