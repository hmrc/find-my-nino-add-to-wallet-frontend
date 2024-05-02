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

import connectors.IndividualDetailsConnector
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.IndividualDetailsRepository
import uk.gov.hmrc.http.HeaderCarrier
import util.Fixtures.{fakeIndividualDetails, fakeIndividualDetailsDataCache}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class IndividualDetailsServiceSpec extends AnyFlatSpec
  with ScalaFutures
  with MockitoSugar {

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.millis) // Increase timeout to 5 seconds



  it should "get individual details" in {
    val jsonResponse = """
{
  "details": {
    "crnIndicator": 1,
    "dateOfBirth": "2000-01-01",
    "nino": "testNino"
  },
  "addressList": {
    "addressLine1": "testAddressLine1",
    "addressLine2": "testAddressLine2",
    "addressLine3": "testAddressLine3",
    "addressLine4": "testAddressLine4",
    "addressLine5": "testAddressLine5",
    "postcode": "testPostcode",
    "country": "testCountry"
  },
  "nameList": {
    "nameType": "testNameType",
    "firstName": "testFirstName",
    "middleName": "testMiddleName",
    "lastName": "testLastName"
  }
}
"""
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    val nino = "testNino"
    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(Future.successful(uk.gov.hmrc.http.HttpResponse(200, jsonResponse)))
    val result = service.fetchIndividualDetails(nino).map(
      _.fold(
        error => Left(error),
        individualDetails => Right(individualDetails)
      )
    )
    assert(result.futureValue.isRight)
  }

  "IndividualDetailsService" should "create individual details data cache" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    val sessionId = "testSessionId"

    when(mockRepository.insertOrReplaceIndividualDetailsDataCache(any)(any[ExecutionContext]))
      .thenReturn(Future.successful("testNino"))
    val result = service.insertOrReplaceIndividualDetailsDataCache(sessionId, fakeIndividualDetails)
    assert(result.isCompleted)
  }

  it should "get ID data from cache for correct NINO" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    val nino = "testNino"
    when(mockRepository.findIndividualDetailsDataByNino(any)(any[ExecutionContext]))
      .thenReturn(Future.successful(Some(fakeIndividualDetailsDataCache)))
    val result = service.getIdDataFromCache(fakeIndividualDetailsDataCache.getNino)
    assert(result.futureValue.isRight)
  }


  it should "get None from cache for incorrect NINO" in {
    val mockRepository = mock[IndividualDetailsRepository]
    val mockConnector = mock[IndividualDetailsConnector]
    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)

    val nino = "testNino"
    when(mockRepository.findIndividualDetailsDataByNino(any)(any[ExecutionContext]))
      .thenReturn(Future.successful(None))
    val result = service.getIdDataFromCache("incorrectNINO")
    assert(result.futureValue.isLeft)
  }

//  it should "handle MongoException" in {
//    val mockRepository = mock[IndividualDetailsRepository]
//    val mockConnector = mock[IndividualDetailsConnector]
//    val service = new IndividualDetailsServiceImpl(mockRepository, mockConnector)
//
//    val nino = "testNino"
//    when(mockRepository.findIndividualDetailsDataByNino(any)(any[ExecutionContext]))
//      .thenThrow(new MongoException("Test exception"))
//
//    val result = service.getIdDataFromCache(nino)
//    assert(result.futureValue.isLeft)
//  }

}