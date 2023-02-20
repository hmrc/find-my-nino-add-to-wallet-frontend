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

package util

import models.{Address, PersonDetails}
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

object AuditUtils {

  val auditSource = "find-my-nino-add-to-wallet-frontend"

  case class YourDetailsAuditEvent(
                                    journeyId: String,
                                    formCreationTimestamp: String,
                                    nino: String,
                                    name: String,
                                    mainAddress: Address,
                                    submissionFromAgent: Boolean = false,
                                    agentCode: Option[String] = None, // Omit if submissionFromAgent is false
                                    device: Option[String],
                                    language: String = "eng",
                                    declareAccurateAndComplete: Boolean = true
                                  )

  object YourDetailsAuditEvent {
    implicit val format: OFormat[YourDetailsAuditEvent] = Json.format[YourDetailsAuditEvent]
  }

  def getLang(hc: HeaderCarrier): String = hc.otherHeaders.toMap.get("Cookie") match {
    case Some(s) => if (s.length > 0 && s.contains(";")) s.replace("; ", ";")
      .split(";").filter(p => p.contains("PLAY_LANG"))(0).split("=").toList(1) else ""
    case _ => ""
  }

  def getUserDevice(hc: HeaderCarrier): String = {
    val strUserAgent = hc.otherHeaders.toMap.getOrElse("User-Agent", "")
    if (strUserAgent.length > 0 && strUserAgent.contains(" ")) {
      strUserAgent.replace("; ", ";").split(" ")(1)
        .replace("(", "")
        .replace(")", "")

    }
    else ""
  }

  def getUserAgent(hc: HeaderCarrier): String = hc.otherHeaders.toMap.getOrElse("User-Agent", "")

  def getPersonAddress(personDetails: PersonDetails): Address = {
    personDetails.address match {
      case Some(a: Address) => a
      case _ => Address.apply(Some(""), Some(""), Some(""), Some(""), Some(""),
        Some(""), Some(""), Some(LocalDate.now()), Some(LocalDate.now()), Some(""), true)
    }
  }

  private def buildDataEvent(auditType: String, transactionName: String, detail: JsValue)(implicit
                                                                                          hc: HeaderCarrier
  ): ExtendedDataEvent = {
    val strPath = hc.otherHeaders.toMap.get("path")
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
        "path" -> strPath
      ).map(x => x._2.map((x._1, _))).flatten.toMap,
      detail = detail
    )
  }

  def buildDetails(personDetails: PersonDetails, journeyId: String, hc: HeaderCarrier): YourDetailsAuditEvent = {
    val person = personDetails.person
    val mainAddress = getPersonAddress(personDetails)
    val strUserAgent = getUserAgent(hc)
    val strLang = getLang(hc)
    val strDevice = getUserDevice(hc)

    YourDetailsAuditEvent(
      journeyId,
      timestamp(),
      person.nino.get.nino,
      name = person.fullName,
      mainAddress = mainAddress,
      agentCode = Some(strUserAgent),
      device = Some(strDevice),
      language = strLang
    )
  }

  private def timestamp(): String =
    java.time.Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)

  def buildViewNinoLandingPageEvent(personDetails: PersonDetails)(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    buildDataEvent(
      "ViewNinoLandingPage",
      "find-my-nino-add-to-wallet-frontend",
      Json.toJson(buildDetails(personDetails, "ViewNinoLandingPage", hc)))
  }

  def buildViewNinoLetterEvent(personDetails: PersonDetails)(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    buildDataEvent(
      "ViewNinoLetter",
      "find-my-nino-add-to-wallet-frontend",
      Json.toJson(buildDetails(personDetails, "ViewNinoLetter", hc)))
  }

  def buildDownloadNinoLetterEvent(personDetails: PersonDetails)(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    buildDataEvent(
      "DownloadNinoLetter",
      "find-my-nino-add-to-wallet-frontend",
      Json.toJson(buildDetails(personDetails, "DownloadNinoLetter", hc)))
  }

  def buildAddNinoToWalletEvent(personDetails: PersonDetails)(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    buildDataEvent(
      "AddNinoToWallet",
      "find-my-nino-add-to-wallet-frontend",
      Json.toJson(buildDetails(personDetails, "AddNinoToWallet", hc)))
  }

  def buildCaptureNinoEvent(personDetails: PersonDetails)(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    buildDataEvent(
      "CaptureNino",
      "find-my-nino-add-to-wallet-frontend",
      Json.toJson(buildDetails(personDetails, "CaptureNino", hc)))
  }
}
