/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.{ApplePassConnector, CitizenDetailsConnector, PersonDetailsSuccessResponse}
import models.StoreMyNino
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import util.CDFixtures
import util.Stubs.userLoggedInFMNUser
import util.TestData.NinoUser
import views.html.StoreMyNinoView

import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreMyNinoControllerSpec extends SpecBase with CDFixtures with MockitoSugar {

  val passId = "applePassId"
  val notApplePassId = ""
  val personDetailsId = "pdId"
  val pd = buildPersonDetails
  val controller = applicationWithConfig.injector.instanceOf[StoreMyNinoController]

  lazy val view = applicationWithConfig.injector.instanceOf[StoreMyNinoView]
  val mockSessionRepository = mock[SessionRepository]
  val mockApplePassConnector = mock[ApplePassConnector]
  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"

  when(mockApplePassConnector.getApplePass(eqTo(passId))(any(),any()))
    .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))
  /*when(mockApplePassConnector.getApplePass(eqTo(notApplePassId))(any(),any()))
    .thenThrow(eqTo(new HttpException("", 404)))*/
  when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))
  when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))
  when(mockApplePassConnector.createApplePass(any(),any())(any(),any()))
    .thenReturn(Future(Some(passId)))
  when(mockApplePassConnector.createPersonDetailsRow(any())(any(),any()))
    .thenReturn(Future(Some(personDetailsId)))
  when(mockCitizenDetailsConnector.personDetails(any())(any()))
    .thenReturn(Future(PersonDetailsSuccessResponse(pd)))
  when(mockApplePassConnector.getQrCode(eqTo(passId))(any(),any()))
    .thenReturn(Future(Some(Base64.getDecoder.decode(fakeBase64String))))
  /*when(mockApplePassConnector.getQrCode(eqTo(notApplePassId))(any(), any()))
    .thenThrow(new HttpException("", 404))*/
  when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))


  "StoreMyNino Controller" - {

    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[ApplePassConnector].toInstance(mockApplePassConnector),
            inject.bind[CitizenDetailsConnector].toInstance(mockCitizenDetailsConnector)
          )
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.onPageLoad().url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsString(result) mustEqual (view(passId, "AA000003B", personDetailsId)(request, messages(application))).toString
      }
    }

    "must return apple pass" in {

      val application = applicationBuilderWithConfig().overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[ApplePassConnector].toInstance(mockApplePassConnector),
          )
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.getPassCard(passId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
      }
    }

    "must return QR code" in {
      val application = applicationBuilderWithConfig()
          .overrides(
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[ApplePassConnector].toInstance(mockApplePassConnector),
          )
          .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.getQrCode(passId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual OK
        contentAsBytes(result) mustEqual Base64.getDecoder.decode(fakeBase64String)
      }
    }

    /*"must return NOT FOUND code" in {
      val application = applicationBuilderWithConfig()
        .overrides(
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[ApplePassConnector].toInstance(mockApplePassConnector),
        )
        .build()

      running(application) {
        userLoggedInFMNUser(NinoUser)
        val request = FakeRequest(GET, routes.StoreMyNinoController.getQrCode(notApplePassId).url)
          .withSession(("authToken", "Bearer 123"))
        val result = route(application, request).value
        status(result) mustEqual NOT_FOUND

      }
    }*/


  }
}
