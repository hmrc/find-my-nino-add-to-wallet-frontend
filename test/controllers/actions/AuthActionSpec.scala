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

package controllers.actions

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.AuthActionSpec.{fakeAuthConnector, retrievals}
import controllers.routes
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with MockitoSugar {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new MissingBearerToken),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new BearerTokenExpired),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new InsufficientEnrolments),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            appConfig,
            bodyParsers
          )
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "the user has a valid auth record" - {

      "must redirect the user to foo" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.get(any())) thenReturn Future.successful(None)

        val application =
          applicationBuilder()
            .overrides(
              inject.bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(fakeAuthConnector(retrievals), appConfig, bodyParsers)
          val controller = new Harness(authAction)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
    }
  }
}

object AuthActionSpec {

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
    ): Future[A] =
      stubbedRetrievalResult.asInstanceOf[Future[A]]
  }

  private def retrievals: Future[Some[String]] = Future.successful(Some("internal-id"))
}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.failed(exceptionToReturn)
}
