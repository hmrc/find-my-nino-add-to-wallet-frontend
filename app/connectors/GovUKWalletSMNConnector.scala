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

package connectors

import com.google.inject.Inject
import config.ConfigDecorator
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse}
import models.{GovUKPassDetails, GovUkPassCreateResponse}

import scala.concurrent.{ExecutionContext, Future}

class GovUKWalletSMNConnector @Inject()(config: ConfigDecorator, http: HttpClient) {

  private val headers: Seq[(String, String)] = Seq("Content-Type" -> "application/json")

  def createGovUKPass(givenName: List[String], familyName: String, nino: String)
                     (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Some[GovUkPassCreateResponse]] = {

    val url = s"${config.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/create-govuk-pass"
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    val govPassDetails =  GovUKPassDetails(givenName, familyName, nino)

    http.POST[JsValue, HttpResponse](url, Json.toJson(govPassDetails))(implicitly, implicitly, hc, implicitly)
      .map { response =>
        response.status match {
          case OK => {
            println(response.body)
            Some(Json.parse(response.body).as[GovUkPassCreateResponse])
          }
          case _  => throw new HttpException(response.body, response.status)
        }
      }
  }

}
