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
import config.FrontendAppConfig
import org.mockito.{ArgumentMatchers, Mockito, MockitoSugar}
import org.scalatest.RecoverMethods
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

class HttpClientResponseSpec extends PlaySpec with MockitoSugar with ScalaFutures with RecoverMethods {

  private val mockLogger: Logger = mock[Logger]
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private lazy val httpClientResponseUsingMockLogger: HttpClientResponse = new HttpClientResponse(mockFrontendAppConfig) {
    override protected val logger: Logger = mockLogger
  }

  private val dummyContent = "error message"
  private val alreadyAnAdultErrorCode = "1234"

  "read" must {
    behave like clientResponseLogger(
      httpClientResponseUsingMockLogger.read,
      infoLevel = Set(BAD_REQUEST),
      infoLevelWithBodyCheck = UNPROCESSABLE_ENTITY,
      errorLevelWithoutThrowable = Set(TOO_MANY_REQUESTS, INTERNAL_SERVER_ERROR),
      errorLevelWithThrowable = Set(UNAUTHORIZED),
    )
  }

  private def clientResponseLogger(
    block: Future[Either[UpstreamErrorResponse, HttpResponse]] => EitherT[Future, UpstreamErrorResponse, HttpResponse],
    infoLevel: Set[Int],
    infoLevelWithBodyCheck: Int,
    errorLevelWithThrowable: Set[Int],
    errorLevelWithoutThrowable: Set[Int]
  ): Unit = {

    infoLevel.foreach { httpResponseCode =>
      s"log message: INFO level only when response code is $httpResponseCode" in {
        reset(mockLogger)
        val response = Future.successful(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
          verifyCalls(info = Some(dummyContent))
        }
      }
    }

    s"log message: INFO level when response is UNPROCESSABLE_ENTITY and body contains $alreadyAnAdultErrorCode" in {
      reset(mockLogger)
      val response = Future.successful(Right(HttpResponse(infoLevelWithBodyCheck, alreadyAnAdultErrorCode)))
      whenReady(block(response).value) { actual =>
        val httpResponse = actual.getOrElse(fail(s"Expected Right(HttpResponse), but got Left($actual)"))
        httpResponse.status mustBe infoLevelWithBodyCheck
        httpResponse.body mustBe alreadyAnAdultErrorCode
        verifyCalls(info = Some("UNPROCESSABLE_ENTITY - alreadyAnAdultErrorCode"))
      }
    }

    errorLevelWithThrowable.foreach { httpResponseCode =>
      s"log message: ERROR level WITH throwable when response code is $httpResponseCode" in {
        reset(mockLogger)
        val response = Future.successful(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
          verifyCalls(errorWithThrowable = Some(dummyContent))
        }
      }
    }

    errorLevelWithoutThrowable.foreach { httpResponseCode =>
      s"log message: ERROR level WITHOUT throwable when response code is $httpResponseCode" in {
        reset(mockLogger)
        val response = Future.successful(Left(UpstreamErrorResponse(dummyContent, httpResponseCode)))
        whenReady(block(response).value) { actual =>
          actual mustBe Left(UpstreamErrorResponse(dummyContent, httpResponseCode))
          verifyCalls(errorWithoutThrowable = Some(dummyContent))
        }
      }
    }

    "log message: ERROR level WITHOUT throwable when future fails with HttpException & recovers to BAD GATEWAY" in {
      reset(mockLogger)
      val response = Future.failed(new HttpException(dummyContent, GATEWAY_TIMEOUT))
      whenReady(block(response).value) { actual =>
        actual mustBe Left(UpstreamErrorResponse(dummyContent, BAD_GATEWAY))
        verifyCalls(errorWithoutThrowable = Some(dummyContent))
      }
    }

    "log nothing when future fails with a non-HttpException" in {
      reset(mockLogger)
      val response = Future.failed(new RuntimeException(dummyContent))
      recoverToSucceededIf[RuntimeException] {
        block(response).value
      }
      verifyCalls()
    }
  }

  private def verifyCalls(
                           info: Option[String] = None,
                           errorWithThrowable: Option[String] = None,
                           errorWithoutThrowable: Option[String] = None
                         ): Unit = {

    def argumentMatcher(content: Option[String]): String = content match {
      case None    => ArgumentMatchers.any()
      case Some(c) => ArgumentMatchers.eq(c)
    }

    Mockito.verify(mockLogger, times(info.map(_ => 1).getOrElse(0)))
      .info(argumentMatcher(info))(ArgumentMatchers.any())

    Mockito.verify(mockLogger, times(errorWithThrowable.map(_ => 1).getOrElse(0)))
      .error(argumentMatcher(errorWithThrowable), ArgumentMatchers.any())(ArgumentMatchers.any())

    Mockito.verify(mockLogger, times(errorWithoutThrowable.map(_ => 1).getOrElse(0)))
      .error(argumentMatcher(errorWithoutThrowable))(ArgumentMatchers.any())
  }
}
