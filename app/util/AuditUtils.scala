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

package util

import models.individualDetails.{AddressData, AddressLine, IndividualDetailsDataCache}
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

object AuditUtils {

  val auditSource = "find-my-nino-add-to-wallet-frontend"

  case class YourDetailsAuditEvent(
                                    journeyId: String,
                                    formCreationTimestamp: String,
                                    nino: String,
                                    name: String,
                                    mainAddress: AddressData,
                                    device: Option[String],
                                    language: String = "en",
                                    WalletProvider: Option[String]
                                  )

  object YourDetailsAuditEvent {
    implicit val format: OFormat[YourDetailsAuditEvent] = Json.format[YourDetailsAuditEvent]
  }

  def getLanguageFromCookieStr(hc: HeaderCarrier): String = hc.otherHeaders.toMap.get("Cookie") match {
    case Some(s) =>
      if (s.length > 0 && s.contains("PLAY_LANG="))
        "PLAY_LANG=([A-Za-z]*)".r.findAllIn(s).group(1)
      else "en"
    case _ => "en"
  }

  def getUserDevice(hc: HeaderCarrier): String = {
    val strUserAgent = hc.otherHeaders.toMap.getOrElse("User-Agent", "")
    if (strUserAgent.length > 0 && strUserAgent.contains(" ")) {
      if(strUserAgent.contains("iPhone"))
        "iOS"
      else
        if(strUserAgent.contains("Android"))
          "Android"
        else
          ""
    }
    else ""
  }

  def getUserAgent(hc: HeaderCarrier): String = hc.otherHeaders.toMap.getOrElse("User-Agent", "")

  def getReferer(hc: HeaderCarrier): String = hc.otherHeaders.toMap.getOrElse("Referer", "")


  private def emptyAddress = AddressData(
    addressLine1 = AddressLine(""),
    addressLine2 = AddressLine(""),
    addressLine3 = None,
    addressLine4 = None,
    addressLine5 = None,
    addressPostcode = None
  )

  def getIndivudualsAddress(individualDetailsDataCache: IndividualDetailsDataCache): AddressData = {
    individualDetailsDataCache.getAddress match {
      case Some(a: AddressData) => a
      case _ => emptyAddress
    }
  }


  private def buildDataEvent(auditType: String, transactionName: String, detail: JsValue)(implicit
                                                                                          hc: HeaderCarrier
  ): ExtendedDataEvent = {
    val strPath = hc.otherHeaders.toMap.get("path")
    val strReferer = getReferer(hc)
    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = Map(
        "transactionName" -> Some(transactionName),
        "X-Session-ID" -> hc.sessionId.map(_.value),
        "X-Request-ID" -> hc.requestId.map(_.value),
        "clientIP" -> hc.trueClientIp,
        "clientPort" -> hc.trueClientPort,
        "deviceID" -> hc.deviceID,
        "path" -> strPath,
        "referer" -> Some(strReferer)
      ).map(x => x._2.map((x._1, _))).flatten.toMap,
      detail = detail
    )
  }

  private def buildDetails(individualDetailsDataCache: IndividualDetailsDataCache, journeyId: String, hc: HeaderCarrier, walletProvider: Option[String]): YourDetailsAuditEvent = {

    val mainAddress = getIndivudualsAddress(individualDetailsDataCache)
    val strLang = getLanguageFromCookieStr(hc)
    val strDevice = getUserDevice(hc)

    individualDetailsDataCache.getNino match {
      case (nino:String) =>
        YourDetailsAuditEvent(
          journeyId,
          timestamp(),
          nino,
          name = individualDetailsDataCache.getFullName,
          mainAddress = mainAddress,
          device = Some(strDevice),
          language = strLang,
          walletProvider
        )
      case _ => throw new NotFoundException("Nino not found for person when building audit event")
    }


  }

  private def timestamp(): String =
    java.time.Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)

  def buildAuditEvent(individualDetailsDataCache: IndividualDetailsDataCache,
                     auditType: String,
                      appName: String,
                      walletProvider: Option[String])(implicit hc: HeaderCarrier): ExtendedDataEvent = {
        buildDataEvent(auditType, s"$appName-$auditType",
          Json.toJson(buildDetails(individualDetailsDataCache, auditType, hc, walletProvider)))
    }

}
