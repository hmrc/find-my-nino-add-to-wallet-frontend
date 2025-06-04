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
import models.individualDetails.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import repositories.IndividualDetailsRepoTrait
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.Fixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CachingIndividualDetailsConnectorSpec extends AnyFlatSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val nino      = "AA123456A"
  val sessionId = "session-123"

  "CachingIndividualDetailsConnector" should "return cached data if present in repository" in {
    val mockUnderlying = mock[IndividualDetailsConnector]
    val mockRepo       = mock[IndividualDetailsRepoTrait]
    val connector      = new CachingIndividualDetailsConnector(mockUnderlying, mockRepo)

    when(mockRepo.findIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(Some(Fixtures.fakeIndividualDetailsDataCache)))

    val result = connector.getIndividualDetails(nino, sessionId).value.futureValue

    result shouldBe Right(Fixtures.fakeIndividualDetailsDataCache)
    verify(mockUnderlying, never).getIndividualDetails(any, any)(any, any)
  }

  it should "fetch from API and cache when not found in repo" in {
    val mockUnderlying = mock[IndividualDetailsConnector]
    val mockRepo       = mock[IndividualDetailsRepoTrait]
    val connector      = new CachingIndividualDetailsConnector(mockUnderlying, mockRepo)

    when(mockRepo.findIndividualDetailsDataByNino(any)(any)).thenReturn(Future.successful(None))
    when(mockUnderlying.getIndividualDetails(any, any)(any, any))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Fixtures.fakeIndividualDetailsDataCache))
    when(mockRepo.insertOrReplaceIndividualDetailsDataCache(any)(any)).thenReturn(Future.successful(nino))

    val result = connector.getIndividualDetails(nino, sessionId).value.futureValue

    result                                         shouldBe a[Right[_, _]]
    result.toOption.get.individualDetailsData.nino shouldBe Fixtures.fakeIndividualDetailsDataCache.individualDetailsData.nino
  }

  it should "return Left if API fails and repo has no data" in {
    val mockUnderlying = mock[IndividualDetailsConnector]
    val mockRepo       = mock[IndividualDetailsRepoTrait]
    val connector      = new CachingIndividualDetailsConnector(mockUnderlying, mockRepo)

    val error = UpstreamErrorResponse("Not found", 404)

    when(mockRepo.findIndividualDetailsDataByNino(any)(any)).thenReturn(Future.successful(None))
    when(mockUnderlying.getIndividualDetails(any, any)(any, any))
      .thenReturn(EitherT.leftT[Future, IndividualDetailsDataCache](error))

    val result = connector.getIndividualDetails(nino, sessionId).value.futureValue

    result shouldBe Left(error)
  }

  it should "return Left with a cache error if repository lookup fails" in {
    val mockUnderlying = mock[IndividualDetailsConnector]
    val mockRepo       = mock[IndividualDetailsRepoTrait]
    val connector      = new CachingIndividualDetailsConnector(mockUnderlying, mockRepo)
    val exception      = new RuntimeException("Failed to connect to MongoDB")

    when(mockRepo.findIndividualDetailsDataByNino(any)(any)).thenReturn(Future.failed(exception))
    val error = UpstreamErrorResponse(s"Cache error: Failed to connect to MongoDB", 500)

    val result = connector.getIndividualDetails(nino, sessionId).value.futureValue

    result shouldBe a[Left[_, _]]
    result shouldBe Left(error)
    verify(mockUnderlying, never).getIndividualDetails(any, any)(any, any)
  }

  it should "return false if delete is not acknowledged" in {
    val mockUnderlying = mock[IndividualDetailsConnector]
    val mockRepo       = mock[IndividualDetailsRepoTrait]
    val connector      = new CachingIndividualDetailsConnector(mockUnderlying, mockRepo)

    when(mockRepo.deleteIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(newDeleteResult(acknowledged = false)))

    connector.deleteIndividualDetails(nino).futureValue shouldBe false
  }

  it should "return true if delete is acknowledged" in {
    val mockUnderlying = mock[IndividualDetailsConnector]
    val mockRepo       = mock[IndividualDetailsRepoTrait]
    val connector      = new CachingIndividualDetailsConnector(mockUnderlying, mockRepo)

    when(mockRepo.deleteIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(newDeleteResult(acknowledged = true)))

    connector.deleteIndividualDetails(nino).futureValue shouldBe true
  }

  private def newDeleteResult(acknowledged: Boolean, deletedCount: Long = 1): org.mongodb.scala.result.DeleteResult =
    new org.mongodb.scala.result.DeleteResult {
      override def wasAcknowledged(): Boolean = acknowledged
      override def getDeletedCount: Long      = deletedCount
    }
}
