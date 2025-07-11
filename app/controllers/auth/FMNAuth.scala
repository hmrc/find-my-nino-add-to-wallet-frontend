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

package controllers.auth

import config.FrontendAppConfig
import connectors.FandFConnector
import controllers.auth.FMNAuth.toContinueUrl
import controllers.routes
import models.NationalInsuranceNumber
import play.api.Logging
import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsFormUrlEncoded, BodyParser, Call, ControllerComponents, Request, RequestHeader, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.{Retrievals, TrustedHelper}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

case class AuthContext[A](
  nino: NationalInsuranceNumber,
  isUser: Boolean,
  internalId: String,
  confidenceLevel: ConfidenceLevel,
  affinityGroup: AffinityGroup,
  allEnrolments: Enrolments,
  trustedHelper: Option[TrustedHelper],
  request: Request[A]
)

trait FMNAuth(val fandfConnector: FandFConnector) extends AuthorisedFunctions with Logging {
  this: FrontendController =>

  protected type FMNAction[A] = AuthContext[A] => Future[Result]
  private val AuthPredicate = AuthProviders(GovernmentGateway)

  def authorisedAsFMNUser(body: FMNAction[Any])(
    loginContinueUrl: Call
  )(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[_], config: FrontendAppConfig): Future[Result] =
    authorisedUser(loginContinueUrl, body)

  def authorisedAsFMNUser(implicit
    ec: ExecutionContext,
    config: FrontendAppConfig,
    cc: ControllerComponents,
    loginContinueUrl: Call
  ): ActionBuilder[AuthContext, AnyContent] =
    new ActionBuilder[AuthContext, AnyContent] {
      override protected def executionContext: ExecutionContext = ec
      override def parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser

      override def invokeBlock[A](request: Request[A], block: AuthContext[A] => Future[Result]): Future[Result] = {
        implicit val req: Request[A]   = request
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        authorisedUser(loginContinueUrl, block)
      }
    }

  private def upliftConfidenceLevel(request: Request[_])(implicit config: FrontendAppConfig): Future[Result] =
    Future.successful(
      Redirect(
        config.identityVerificationUpliftUrl,
        Map(
          "origin"          -> Seq(config.defaultOrigin.origin),
          "confidenceLevel" -> Seq(ConfidenceLevel.L200.toString),
          "completionURL"   -> Seq(
            config.saveYourNationalNumberFrontendHost +
              routes.ApplicationController.showUpliftJourneyOutcome(Some(RedirectUrl(request.uri)))
          ),
          "failureURL"      -> Seq(
            config.saveYourNationalNumberFrontendHost +
              routes.ApplicationController.showUpliftJourneyOutcome(Some(RedirectUrl(request.uri)))
          )
        )
      )
    )

  private def upliftCredentialStrength()(implicit config: FrontendAppConfig): Future[Result] =
    Future.successful(
      Redirect(
        config.multiFactorAuthenticationUpliftUrl,
        Map(
          "origin"      -> Seq(config.defaultOrigin.origin),
          "continueUrl" -> Seq(config.saveYourNationalNumberFrontendHost + "/save-your-national-insurance-number")
        )
      )
    )

  private object GTOE200 {
    def unapply(confLevel: ConfidenceLevel): Option[ConfidenceLevel] =
      if (confLevel.level >= ConfidenceLevel.L200.level) Some(confLevel) else None
  }

  private object LT200 {
    def unapply(confLevel: ConfidenceLevel): Option[ConfidenceLevel] =
      if (confLevel.level < ConfidenceLevel.L200.level) Some(confLevel) else None
  }

  private val FMNRetrievals =
    Retrievals.nino and
      Retrievals.affinityGroup and
      Retrievals.allEnrolments and
      Retrievals.credentials and
      Retrievals.credentialStrength and
      Retrievals.confidenceLevel and
      Retrievals.profile and
      Retrievals.internalId and
      Retrievals.credentialRole

  private def enrolmentAffinityCheck[A](
    enrolments: Set[Enrolment],
    affinityGroup: AffinityGroup,
    nino: String,
    block: FMNAction[A]
  )(implicit config: FrontendAppConfig, authenticatedRequest: AuthContext[A]) =
    (checkPTAEnrolment(enrolments, domain.Nino(nino)), affinityGroup == AffinityGroup.Agent) match {
      case (false, _)   =>
        val redirectUrl = config.getTaxEnrolmentAssignmentRedirectUrl(
          config.saveYourNationalNumberFrontendHost + "/save-your-national-insurance-number"
        )
        Future.successful(Redirect(redirectUrl))
      case (true, true) =>
        logger.warn("Agent affinity group encountered whilst attempting to authorise user")
        Future successful Redirect(controllers.routes.UnauthorisedController.onPageLoad)
      case _            =>
        block(authenticatedRequest)
    }

  private def authorisedUser[A](loginContinueUrl: Call, block: FMNAction[A])(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier,
    config: FrontendAppConfig,
    request: Request[A]
  ): Future[Result] =
    authorised(AuthPredicate)
      .retrieve(FMNRetrievals) {
        case _ ~ Some(Individual | Organisation) ~ _ ~ _ ~ (Some(CredentialStrength.weak) |
            Some("none")) ~ _ ~ _ ~ _ ~ _ =>
          upliftCredentialStrength()

        case _ ~ Some(Individual | Organisation) ~ _ ~ _ ~ _ ~ LT200(_) ~ _ ~ _ ~ _ =>
          upliftConfidenceLevel(request)

        case Some(nino) ~
            Some(affinityGroup) ~ Enrolments(enrolments) ~ _ ~ Some(CredentialStrength.strong) ~
            GTOE200(confidenceLevel) ~ _ ~ Some(internalId) ~ Some(_) =>
          fandfConnector.getTrustedHelper().flatMap { helper =>
            val trimmedRequest: Request[A] = request
              .map {
                case AnyContentAsFormUrlEncoded(data) =>
                  AnyContentAsFormUrlEncoded(data.map { case (key, vals) =>
                    (key, vals.map(_.trim))
                  })
                case b                                => b
              }
              .asInstanceOf[Request[A]]

            implicit val authenticatedRequest: AuthContext[A] = AuthContext[A](
              NationalInsuranceNumber(helper.fold(nino)(helper => helper.principalNino.get)),
              isUser = true,
              internalId,
              confidenceLevel,
              affinityGroup,
              Enrolments(enrolments),
              helper,
              trimmedRequest
            )

            enrolmentAffinityCheck(enrolments, affinityGroup, nino, block)
          }

        case _ =>
          logger.warn("All authorize checks failed whilst attempting to authorise user")
          Future successful Redirect(controllers.routes.UnauthorisedController.onPageLoad)
      }
      .recover {
        handleFailure(toContinueUrl(loginContinueUrl))
      }

  private def handleFailure(
    loginContinueUrl: String
  )(implicit config: FrontendAppConfig): PartialFunction[Throwable, Result] = {

    case _: NoActiveSession =>
      logger.debug("no active session whilst attempting to authorise user: redirecting to login")
      Redirect(config.loginUrl, Map("continue" -> Seq(loginContinueUrl), "origin" -> Seq(config.appName)))

    case IncorrectNino =>
      logger.warn("incorrect NINO encountered whilst attempting to authorise user")
      Redirect(controllers.routes.UnauthorisedController.onPageLoad)

    case _: UpstreamErrorResponse =>
      logger.warn(s"Upstream error due to Auth service connection refused.")
      val retryUrl = routes.NinoLetterController.onPageLoad.url
      Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(retryUrl))))

    case ex: AuthorisationException =>
      logger.warn(s"could not authenticate user due to: $ex")
      InternalServerError
  }

  private def checkPTAEnrolment(enrolments: Set[Enrolment], sessionNino: domain.Nino): Boolean =
    enrolments
      .filter(_.key == "HMRC-PT")
      .flatMap { enrolment =>
        enrolment.identifiers
          .filter(id => id.key == "NINO")
      } match {
      case enrolmentIdentifiers
          if enrolmentIdentifiers
            .exists(enrolmentIdentifier => domain.Nino(enrolmentIdentifier.value) == sessionNino) =>
        true
      case _ =>
        logger.error(
          "The nino in HMRC-PT enrolment does not match the one from the user session or no enrolment exists for this nino"
        )
        false
    }

}

object FMNAuth {
  def toContinueUrl(call: Call)(implicit rh: RequestHeader): String =
    if (call.absoluteURL().contains("://localhost")) {
      call.absoluteURL() + rh.uri.replaceFirst("/", "")
    } else {
      call.url + rh.uri.replaceFirst("/", "")
    }

}
