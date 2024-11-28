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

package services

import com.mongodb.client.result.DeleteResult
import connectors.IndividualDetailsConnector
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito._
import org.mongodb.scala.MongoException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.libs.json.Json
import repositories.IndividualDetailsRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.Fixtures.{fakeIndividualDetails, fakeIndividualDetailsWithKnownAsName, fakeIndividualDetailsWithoutMiddleName, individualRespJsonInvalid}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class IndividualDetailsServiceSpec extends AnyFlatSpec
  with ScalaFutures
  with MockitoSugar {

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 50.millis) // Increase timeout to 5 seconds

  "IndividualDetailsService" should "create individual details data cache when the cache has expired and there is a valid nino" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    val fakeIndividualDetailsJson = Json.toJson(fakeIndividualDetails).toString()


    when(mockRepository.findIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(None))
    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(Future.successful(HttpResponse(200, fakeIndividualDetailsJson)))
    when(mockRepository.insertOrReplaceIndividualDetailsDataCache(any)(any[ExecutionContext]))
      .thenReturn(Future.successful("testNino"))

    val result = service.getIdDataFromCache("testNino", "some-fake-Id")

    assert(result.futureValue isRight)
    assert(result.futureValue.fold( _ => false, _.individualDetailsData.nino == "AB123456C"))
    assert(result.futureValue.fold(_ => false, _.individualDetailsData.fullName == "Dr FIRSTNAME MIDDLENAME LASTNAME PhD"))
  }

  "IndividualDetailsService" should "create individual details data cache where no middle name present" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    val fakeIndividualDetailsJson = Json.toJson(fakeIndividualDetailsWithoutMiddleName).toString()

    when(mockRepository.findIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(None))
    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(Future.successful(HttpResponse(200, fakeIndividualDetailsJson)))
    when(mockRepository.insertOrReplaceIndividualDetailsDataCache(any)(any[ExecutionContext]))
      .thenReturn(Future.successful("testNino"))

    val result = service.getIdDataFromCache("testNino", "some-fake-Id")

    assert(result.futureValue isRight)
    assert(result.futureValue.fold(_ => false, _.individualDetailsData.nino == "AB123456C"))
    assert(result.futureValue.fold(_ => false, _.individualDetailsData.fullName == "Dr FIRSTNAME LASTNAME PhD"))
  }

  "IndividualDetailsService" should "create individual details data cache where the known as name is present" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    val fakeIndividualDetailsJson = Json.toJson(fakeIndividualDetailsWithKnownAsName).toString()

    when(mockRepository.findIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(None))
    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(Future.successful(HttpResponse(200, fakeIndividualDetailsJson)))
    when(mockRepository.insertOrReplaceIndividualDetailsDataCache(any)(any[ExecutionContext]))
      .thenReturn(Future.successful("testNino"))

    val result = service.getIdDataFromCache("testNino", "some-fake-Id")

    assert(result.futureValue isRight)
    assert(result.futureValue.fold(_ => false, _.individualDetailsData.nino == "AB123456C"))
    assert(result.futureValue.fold(_ => false, _.individualDetailsData.fullName == "Dr KNOWN AS NAME PhD"))
  }

  "IndividualDetailsService" should "return a left of unprcessible entity where invalid json is returned" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    when(mockRepository.findIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(None))
    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(Future.successful(HttpResponse(OK, individualRespJsonInvalid)))

    val result = service.getIdDataFromCache("testNino", "some-fake-Id")

    assert(result.futureValue isLeft)
  }

  "IndividualDetailsService" should "get None from cache for non-existent NINO in cache and from 1694API" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    when(mockRepository.findIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(None))

    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(Future.successful(HttpResponse(404, "Not found")))

    val result = service.getIdDataFromCache("testNino","testSessionId")
    assert(result.futureValue.isLeft)
  }

  "IndividualDetailsService" should "return true when an entry has been deleted from the cache" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    when(mockRepository.deleteIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.successful(DeleteResult.acknowledged(1)))

    val result = service.deleteIdDataFromCache("testNino")
    assert(result.futureValue)
  }

  "IndividualDetailsService" should "handle MongoException" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    val nino = "testNino"
    when(mockRepository.findIndividualDetailsDataByNino(any)(any))
      .thenReturn(Future.failed(new MongoException("MongoException")))

    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(Future.successful(HttpResponse(404, "Not found")))

    val result = service.getIdDataFromCache(nino, "testSessionId")
    assert(result.futureValue.isLeft)
  }

}