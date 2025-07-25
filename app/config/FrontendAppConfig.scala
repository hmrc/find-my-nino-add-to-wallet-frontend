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

package config

import com.google.inject.{Inject, Singleton}
import controllers.bindable.Origin
import play.api.Configuration
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URLEncoder

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = configuration.get[String]("appName")

  lazy val gtmContainer: String    = configuration.get[String]("tracking-consent-frontend.gtm.container")
  val enc                          = URLEncoder.encode(_: String, "UTF-8")
  lazy val generalQueriesUrl       = "https://www.gov.uk/contact-hmrc"
  lazy val enquiriesForIndividuals =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/national-insurance-enquiries-for-employees-and-individuals"
  val serviceName                  = "save-your-national-insurance-number"

  def getBasGatewayFrontendSignOutUrl(continueUrl: String): String =
    basGatewayFrontendHost + s"/bas-gateway/sign-out-without-state?continue=$continueUrl"

  private def getExternalUrl(serviceName: String): String =
    configuration.get[String](s"external-url.$serviceName.host") +
      configuration.getOptional[String](s"external-url.$serviceName.path").getOrElse("")

  def getFeedbackSurveyUrl(origin: Origin): String =
    feedbackSurveyFrontendHost + "/feedback/" + enc(origin.origin)

  lazy val feedbackSurveyFrontendHost = getExternalUrl("feedback-survey-frontend")

  val loginUrl: String                  = configuration.get[String]("urls.login")
  val loginContinueUrl: String          = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String                = configuration.get[String]("sca-wrapper.signout.url")
  lazy val findMyNinoServiceUrl: String = servicesConfig.baseUrl("find-my-nino-add-to-wallet-service")
  lazy val fandfHost: String            = servicesConfig.baseUrl("fandf")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val cacheTtl: Long                  = configuration.get[Int]("mongodb.timeToLiveInSeconds")
  val individualDetailsCacheTtl: Long = configuration.get[Int]("mongodb.individualDetailsTtlInSeconds")

  val encryptionKey: String = configuration.get[String]("mongodb.encryption.key")

  val crnUpliftAPIAlreadyAdultErrorCode: String = configuration.get[String]("crnUpliftAPI.alreadyAnAdultErrorCode")

  lazy val googleWalletEnabled: Boolean =
    configuration.getOptional[Boolean]("features.google-wallet-enabled").getOrElse(false)
  lazy val appleWalletEnabled: Boolean  =
    configuration.getOptional[Boolean]("features.apple-wallet-enabled").getOrElse(false)
  lazy val crnUpliftEnabled: Boolean    = configuration.getOptional[Boolean]("features.crn-uplift-enabled").getOrElse(true)

  lazy val basGatewayFrontendHost: String     = getExternalUrl("bas-gateway-frontend")
  lazy val multiFactorAuthenticationUpliftUrl = s"$basGatewayFrontendHost/bas-gateway/uplift-mfa"

  lazy val pertaxFrontendHost                             = getExternalUrl("pertax-frontend")
  lazy val checkNationalInsuranceRecordAndPension: String =
    s"$pertaxFrontendHost/personal-account/your-national-insurance-state-pension"

  lazy val origin: String = configuration.getOptional[String]("sosOrigin").orElse(Some(appName)).getOrElse("undefined")

  private lazy val identityVerificationHost: String   = getExternalUrl("identity-verification")
  private lazy val identityVerificationPrefix: String =
    configuration.get[String]("external-url.identity-verification.prefix")
  lazy val identityVerificationUpliftUrl              = s"$identityVerificationHost/$identityVerificationPrefix/uplift"
  val defaultOrigin: Origin                           = Origin("STORE_MY_NINO")
  lazy val saveYourNationalNumberFrontendHost: String =
    getExternalUrl("save-your-national-insurance-number-frontend")

  private lazy val taxEnrolmentAssignmentFrontendHost: String =
    getExternalUrl("tax-enrolment-assignment-frontend")

  def getTaxEnrolmentAssignmentRedirectUrl(url: String): String =
    s"$taxEnrolmentAssignmentFrontendHost/protect-tax-info?redirectUrl=${enc(url)}"

  lazy val individualDetailsProtocol: String =
    configuration.get[String]("microservice.services.individual-details.protocol")
  lazy val individualDetailsHost: String     = configuration.get[String]("microservice.services.individual-details.host")
  lazy val individualDetailsPort: String     = configuration.get[String]("microservice.services.individual-details.port")
  val individualDetailsServiceUrl: String    =
    s"$individualDetailsProtocol://$individualDetailsHost:$individualDetailsPort"

  lazy val ninoByPost: String =
    getExternalUrl("national-insurance-number-letter-spike-frontend")

}
