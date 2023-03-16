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

package config

import com.google.inject.{Inject, Singleton}
import controllers.bindable.Origin
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URLEncoder

@Singleton
class ConfigDecorator @Inject()(configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  val labelServiceName = configuration.get[String]("contact-frontend.host")
  val serviceName = "save-your-national-insurance-number"
  val serviceNamePTA = "Personal tax account"

  val gtmContainer: String = configuration.get[String]("tracking-consent-frontend.gtm.container")
  lazy val trackingHost: String                = getExternalUrl(s"tracking-frontend.host").getOrElse("")
  lazy val trackingServiceUrl = s"$trackingHost/track"
  val enc = URLEncoder.encode(_: String, "UTF-8")

  val loginUrl: String = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String = configuration.get[String]("urls.signOut")
  lazy val findMyNinoServiceUrl: String = servicesConfig.baseUrl("find-my-nino-add-to-wallet-service")
  lazy val citizenDetailsServiceUrl: String = servicesConfig.baseUrl("citizen-details-service")

  lazy val basGatewayFrontendHost = getExternalUrl(s"bas-gateway-frontend.host").getOrElse("")
  lazy val taxEnrolmentAssignmentFrontendHost = getExternalUrl(s"tax-enrolment-assignment-frontend.host").getOrElse("")
  lazy val pertaxFrontendHost = getExternalUrl(s"pertax-frontend.host").getOrElse("")
  lazy val pertaxFrontendForAuthHost = getExternalUrl(s"pertax-frontend.auth-host").getOrElse("")
  lazy val feedbackSurveyFrontendHost = getExternalUrl(s"feedback-survey-frontend.host").getOrElse("")
  val defaultOrigin: Origin = Origin("STORE_MY_NINO")

  private def getExternalUrl(key: String): Option[String] =
    configuration.getOptional[String](s"external-url.$key")

  def getFeedbackSurveyUrl(origin: Origin): String =
    feedbackSurveyFrontendHost + "/feedback/" + enc(origin.origin)


  def getBasGatewayFrontendSignOutUrl(continueUrl: String): String =
    basGatewayFrontendHost + s"/bas-gateway/sign-out-without-state?continue=$continueUrl"

  val exitSurveyUrl: String             = s"$feedbackSurveyFrontendHost/feedback/$serviceName"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  lazy val accessibilityStatementToggle: Boolean =
    configuration.getOptional[Boolean](s"accessibility-statement.toggle").getOrElse(false)
  lazy val accessibilityBaseUrl: String = servicesConfig.getString("accessibility-statement.baseUrl")
  lazy private val accessibilityRedirectUrl =
    servicesConfig.getString("accessibility-statement.redirectUrl")

  def accessibilityStatementUrl(referrer: String) =
    s"$accessibilityBaseUrl/accessibility-statement$accessibilityRedirectUrl?referrerUrl=${SafeRedirectUrl(accessibilityBaseUrl + referrer).encodedUrl}"


}
