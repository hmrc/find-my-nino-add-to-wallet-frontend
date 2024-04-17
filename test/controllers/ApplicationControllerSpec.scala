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

package controllers

import base.SpecBase
import cats.data.EitherT
import config.FrontendAppConfig
import connectors.{CitizenDetailsConnector, IdentityVerificationFrontendConnector, PersonDetailsSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{FailedMatching, IdentityVerificationFrontendService, IdentityVerificationResponse, Incomplete, InsufficientEvidence, LockedOut, PrecondFailed, Success, TechnicalIssue, Timeout, UserAborted}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.HttpResponse
import util.{CDFixtures, Fixtures, UserDetails}
import views.html.identity._
import cats.instances.future._
import models.{ActivatedOnlineFilerSelfAssessmentUser, PersonDetails, SelfAssessmentUserType}
import uk.gov.hmrc.domain.{Nino, SaUtr, SaUtrGenerator}
import util.Fixtures.buildFakeRequestWithAuth

import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends SpecBase with CDFixtures with MockitoSugar {

  override protected def beforeEach(): Unit = {
    reset(mockScaWrapperDataConnector)
    when(mockScaWrapperDataConnector.wrapperData()(any(), any(), any()))
      .thenReturn(Future.successful(wrapperDataResponse))

    reset(mockSessionRepository)
    when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

    reset(mockCitizenDetailsConnector)
    when(mockCitizenDetailsConnector.personDetails(any())(any(), any()))
      .thenReturn(Future(PersonDetailsSuccessResponse(pd)))

    reset(mockIdentityVerificationFrontendConnector)
    when(mockIdentityVerificationFrontendConnector.getIVJourneyStatus(any())(any(), any()))
      .thenReturn(cats.data.EitherT.right[UpstreamErrorResponse](Future.successful(HttpResponse(OK, ""))))
  }

  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockIdentityVerificationFrontendConnector: IdentityVerificationFrontendConnector = mock[IdentityVerificationFrontendConnector]
  val mockIdentityVerificationFrontendService: IdentityVerificationFrontendService = mock[IdentityVerificationFrontendService]

  val pd = buildPersonDetails
  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  trait LocalSetup {

    lazy val authProviderType: String                                                                         = UserDetails.GovernmentGatewayAuthProvider
    lazy val nino: Nino                                                                                       = Fixtures.fakeNino
    lazy val personDetailsResponse: PersonDetails                                                             = Fixtures.buildPersonDetails
    lazy val withPaye: Boolean                                                                                = true
    //lazy val year                                                                                             = current.currentYear
    lazy val getIVJourneyStatusResponse: EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse] =
      EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(Success)))
    lazy val getCitizenDetailsResponse                                                                        = true
    lazy val getSelfAssessmentServiceResponse: SelfAssessmentUserType                                         = ActivatedOnlineFilerSelfAssessmentUser(
      SaUtr(new SaUtrGenerator().nextSaUtr.utr)
    )

    lazy val application = applicationBuilderWithConfig()
      .overrides(
        inject.bind[SessionRepository].toInstance(mockSessionRepository),
        inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector),
        inject.bind[IdentityVerificationFrontendConnector].toInstance(mockIdentityVerificationFrontendConnector)
      )
      .configure("features.sca-wrapper-enabled" -> false)
      .build()

    def controller: ApplicationController =
      new ApplicationController(
        mockIdentityVerificationFrontendService,
        injected[AuthConnector],
        injected[SuccessView],
        injected[CannotConfirmIdentityView],
        injected[FailedIvIncompleteView],
        injected[LockedOutView],
        injected[TimeOutView],
        injected[TechnicalIssuesView]
      )(config, env, ec, injected[MessagesControllerComponents], mock[FrontendAppConfig])

    when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any())) thenReturn {
      getIVJourneyStatusResponse
    }

    def routeWrapper[T](req: FakeRequest[AnyContentAsEmpty.type]): Option[Future[Result]] = {
      controller //Call to inject mocks
      route(app, req)
    }
  }

  "ApplicationController" - {

    "Calling uplift" - {

      "return BAD_REQUEST status when completionURL is empty" in new LocalSetup {

        val result =
          routeWrapper(buildFakeRequestWithAuth("GET", "/save-your-national-insurance-number/do-uplift?redirectUrl=")).get

        status(result) mustBe BAD_REQUEST
        redirectLocation(result) mustBe None

      }

      "return BAD_REQUEST status when completionURL is not relative" in new LocalSetup {

        val result =
          routeWrapper(buildFakeRequestWithAuth("GET", "/save-your-national-insurance-number/do-uplift?redirectUrl=http://example.com")).get

        status(result) mustBe BAD_REQUEST
        redirectLocation(result) mustBe None

      }
    }

    "showUpliftJourneyOutcome" - {

      "redirect to StoreMyNinoController on successful uplift" in new LocalSetup {

        running(application) {
          val request = FakeRequest("GET", "/uplift")
          val result = controller.uplift(None)(request)
          val expectedRedirectUrl = routes.StoreMyNinoController.onPageLoad.url

          assert(status(result) == SEE_OTHER)
          assert(redirectLocation(result).contains(expectedRedirectUrl))
        }
      }

      "showUpliftJourneyOutcome should return Ok when IV journey status is Success" in new LocalSetup {

        running(application) {
          val request = FakeRequest(GET, "/uplift?journeyId=XXXXX").withSession("sessionId" -> "FAKE_SESSION_ID")
          val result = controller.showUpliftJourneyOutcome(None)(request)

          assert(status(result) == OK)
        }

      }

      "showUpliftJourneyOutcome should return LockedOut when IV journey status is LockedOut" in new LocalSetup {

        running(application) {
          when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(LockedOut))))

          val request = FakeRequest(GET, "?journeyId=XXXXX&token=XXXXXX")
          val result = controller.showUpliftJourneyOutcome(None)(request)

          assert(status(result) == UNAUTHORIZED)
        }
      }

      "showUpliftJourneyOutcome should return Unauthorized when IV journey status is UserAborted" in new LocalSetup {

        running(application) {
          when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(UserAborted))))

          val request = FakeRequest(GET, "?journeyId=XXXXX&token=XXXXXX")
          val result = controller.showUpliftJourneyOutcome(None)(request)

          assert(status(result) == UNAUTHORIZED)
        }
      }

      "showUpliftJourneyOutcome should return Unauthorized when IV journey status is Incomplete" in new LocalSetup {

        running(application) {
          when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(Incomplete))))

          val request = FakeRequest(GET, "?journeyId=XXXXX&token=XXXXXX")
          val result = controller.showUpliftJourneyOutcome(None)(request)

          assert(status(result) == UNAUTHORIZED)
        }
      }

      "showUpliftJourneyOutcome should return Unauthorized when IV journey status is PrecondFailed" in new LocalSetup {

        running(application) {
          when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(PrecondFailed))))

          val request = FakeRequest(GET, "?journeyId=XXXXX&token=XXXXXX")
          val result = controller.showUpliftJourneyOutcome(None)(request)

          assert(status(result) == UNAUTHORIZED)
        }
      }

      "showUpliftJourneyOutcome should return Unauthorized when IV journey status is InsufficientEvidence" in new LocalSetup {

        running(application) {
          when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(InsufficientEvidence))))

          val request = FakeRequest(GET, "?journeyId=XXXXX&token=XXXXXX")
          val result = controller.showUpliftJourneyOutcome(None)(request)

          assert(status(result) == UNAUTHORIZED)
        }
      }

      "showUpliftJourneyOutcome should return Unauthorized when IV journey status is FailedMatching" in new LocalSetup {

        running(application) {
          when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(FailedMatching))))

          val request = FakeRequest(GET, "?journeyId=XXXXX&token=XXXXXX")
          val result = controller.showUpliftJourneyOutcome(None)(request)

          assert(status(result) == UNAUTHORIZED)
        }
      }

      "showUpliftJourneyOutcome should return TechnicalIssue(424) when IV journey outcome was TechnicalIssues(500)" in new LocalSetup {

        override lazy val getIVJourneyStatusResponse
        : EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse] =
          EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(TechnicalIssue)))
        running(application) {
          when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(TechnicalIssue))))

          val result = controller.showUpliftJourneyOutcome(None)(buildFakeRequestWithAuth("GET", "/?journeyId=XXXXX"))
          status(result) mustBe FAILED_DEPENDENCY
        }

      }

      "showUpliftJourneyOutcome should return Timeout when IV journey status is UserAborted" in new LocalSetup {

        running(application) {
          when(mockIdentityVerificationFrontendService.getIVJourneyStatus(any())(any(), any()))
            .thenReturn(EitherT[Future, UpstreamErrorResponse, IdentityVerificationResponse](Future.successful(Right(Timeout))))

          val request = FakeRequest(GET, "?journeyId=XXXXX&token=XXXXXX")
          val result = controller.showUpliftJourneyOutcome(None)(request)

          assert(status(result) == UNAUTHORIZED)
        }
      }

      "return bad request when continueUrl is not relative" in new LocalSetup {
        val result = routeWrapper(
          buildFakeRequestWithAuth(
            "GET",
            "/save-your-national-insurance-number/identity-check-complete?continueUrl=http://example.com&journeyId=XXXXX"
          )
        ).get

        status(result) mustBe BAD_REQUEST

      }
    }
  }
}