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
import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import models.individualDetails.*
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualDetailsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: FrontendAppConfig,
  httpClientResponse: HttpClientResponse
) extends Logging {

  def getIndividualDetails(nino: String, sessionId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, IndividualDetails] = {
    val url =
      s"${appConfig.individualDetailsServiceUrl}/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"

    httpClientResponse
      .read(
        httpClient
          .get(url"$url")
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
//      .map { _ =>
//        // Temp code while testing:-
//        val hardCodedTestJson = Json
//          .parse("""{
//                    |   "title":"Mr",
//                    |   "firstForename":"Martin",
//                    |   "surname":"Hempton",
//                    |   "dateOfBirth":"1948-04-23",
//                    |   "nino":"AB216913B",
//                    |   "address":{
//                    |      "addressLine1":"88 TESTING ROAD",
//                    |      "addressCountry":"GREAT BRITAIN",
//                    |      "addressLine3":"TESTREGION",
//                    |      "addressLine2":"TESTTOWN",
//                    |      "addressType":1,
//                    |      "addressLine5":"TESTSHIRE",
//                    |      "addressStartDate":"2003-04-30",
//                    |      "addressPostcode":"EC4 2AA",
//                    |      "addressLine4":"TESTAREA"
//                    |   },
//                    |   "crnIndicator":"false"
//                    |}""".stripMargin)
//          .as[JsObject]
//        hardCodedTestJson.as[IndividualDetails]
//      }
      .map(_.json.as[IndividualDetails])
  }

  // TODO: FIX
  def deleteIndividualDetails(nino: String)(implicit ec: ExecutionContext): Future[Boolean] =
    Future.successful(true)
}
