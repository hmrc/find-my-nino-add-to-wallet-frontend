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
import connectors.IndividualDetailsConnector
import models.individualDetails.IndividualDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.Fixtures.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class IndividualDetailsServiceSpec extends AnyFlatSpec with ScalaFutures with MockitoSugar {

  implicit val hc: HeaderCarrier               = mock[HeaderCarrier]
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 50.millis)

  "IndividualDetailsService" should "return data from connector when cache exists or is fetched" in {
    val mockConnector = mock[IndividualDetailsConnector]
    val service       = new IndividualDetailsServiceImpl(mockConnector)

    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](fakeIndividualDetails))

    val result = service.getIdData("testNino", "testSessionId").value

    result.futureValue shouldBe Right(fakeIndividualDetails)
  }

  it should "propagate UpstreamErrorResponse when connector returns an error" in {
    val mockConnector = mock[IndividualDetailsConnector]
    val service       = new IndividualDetailsServiceImpl(mockConnector)
    val error         = UpstreamErrorResponse("Not found", 404)

    when(mockConnector.getIndividualDetails(any, any)(any, any))
      .thenReturn(EitherT.leftT[Future, IndividualDetails](error))

    val result = service.getIdData("testNino", "testSessionId").value

    result.futureValue shouldBe Left(error)
  }

  it should "delegate delete to connector and return unit" in {
    val mockConnector = mock[IndividualDetailsConnector]
    val service       = new IndividualDetailsServiceImpl(mockConnector)

    when(mockConnector.deleteIndividualDetails(any)(any, any))
      .thenReturn(EitherT(Future.successful(Right((): Unit))))

    val result = Await.result(service.deleteIdData("testNino").value, Duration.Inf)
    result shouldBe Right((): Unit)
  }

}
