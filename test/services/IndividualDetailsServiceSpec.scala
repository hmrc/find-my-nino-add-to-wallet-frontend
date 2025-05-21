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

package services

import cats.data.EitherT
import cats.instances.future.*
import connectors.CachingIndividualDetailsConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.Fixtures.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class IndividualDetailsServiceSpec extends AnyFlatSpec with ScalaFutures with MockitoSugar {

  implicit val hc: HeaderCarrier               = HeaderCarrier()
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.millis)

  "IndividualDetailsService" should "return cached data when available" in {
    val mockConnector = mock[CachingIndividualDetailsConnector]
    val service       = new IndividualDetailsServiceImpl(mockConnector)

    when(mockConnector.getIndividualDetailsWithCache(any[String], any[String])(any))
      .thenReturn(EitherT.rightT(fakeIndividualDetailsDataCache))

    val result = service.getIdData("AB123456C", "session-123").value
    result.futureValue shouldBe Right(fakeIndividualDetailsDataCache)
  }

  "IndividualDetailsService" should "return error if connector returns Left" in {
    val mockConnector = mock[CachingIndividualDetailsConnector]
    val service       = new IndividualDetailsServiceImpl(mockConnector)

    val error = UpstreamErrorResponse("Not found", 404)
    when(mockConnector.getIndividualDetailsWithCache(any[String], any[String])(any))
      .thenReturn(EitherT.leftT(error))

    val result = service.getIdData("AB123456C", "session-123").value
    result.futureValue shouldBe Left(error)
  }

  "IndividualDetailsService" should "delete data and return true when acknowledged" in {
    val mockConnector = mock[CachingIndividualDetailsConnector]
    val service       = new IndividualDetailsServiceImpl(mockConnector)

    when(mockConnector.deleteIndividualDetailsCache(any[String]))
      .thenReturn(Future.successful(true))

    val result = service.deleteIdData("AB123456C")
    result.futureValue shouldBe true
  }

  "IndividualDetailsService" should "delete data and return false when not acknowledged" in {
    val mockConnector = mock[CachingIndividualDetailsConnector]
    val service       = new IndividualDetailsServiceImpl(mockConnector)

    when(mockConnector.deleteIndividualDetailsCache(any[String]))
      .thenReturn(Future.successful(false))

    val result = service.deleteIdData("AB123456C")
    result.futureValue shouldBe false
  }
}

