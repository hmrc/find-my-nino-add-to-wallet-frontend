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

package controllers.actions

import com.github.tomakehurst.wiremock.client.WireMock.*
import controllers.auth.AuthContext
import models.NationalInsuranceNumber
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import util.Fixtures.fakeIndividualDetailsDataCache
import util.WireMockHelper

import scala.concurrent.{ExecutionContext, Future}

class ActionHelperISpec
    extends AnyWordSpec
    with GuiceOneAppPerSuite
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with Matchers {

  override def fakeApplication(): Application = {
    server.start()
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.find-my-nino-add-to-wallet-service.port" -> server.port(),
        "microservice.services.find-my-nino-add-to-wallet-service.host" -> "127.0.0.1",
        "microservice.services.individual-details.port"                 -> server.port(),
        "microservice.services.individual-details.host"                 -> "127.0.0.1",
        "features.crn-uplift-enabled"                                   -> true
      )
      .build()
  }

  implicit val hc: HeaderCarrier             = HeaderCarrier()
  implicit val ec: ExecutionContext          = app.injector.instanceOf[ExecutionContext]
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val nino: String                           = new Generator().nextNino.nino
  val sessionId: String                      = "session-id"
  val messages: Messages                     = MessagesImpl(Lang("en"), messagesApi).messages

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "").withSession(SessionKeys.sessionId -> sessionId)

  def fakeAuthContext: AuthContext[AnyContent] = AuthContext(
    nino = NationalInsuranceNumber(nino),
    isUser = true,
    internalId = "test-id",
    confidenceLevel = ConfidenceLevel.L200,
    affinityGroup = AffinityGroup.Individual,
    allEnrolments = Enrolments(Set(Enrolment("HMRC-PT"))),
    trustedHelper = None,
    request = fakeRequest
  )

  lazy val actionHelper: ActionHelper = app.injector.instanceOf[ActionHelper]

  val individualDetailsCrnTrueJson: String = Json
    .toJson(
      fakeIndividualDetailsDataCache
        .copy(individualDetailsData = fakeIndividualDetailsDataCache.individualDetailsData.copy(crnIndicator = "true"))
    )
    .toString()

  "ActionHelper.checkForCrn" must {
    "return 500 with TechnicalIssuesNoRetryView when individual details API fails with Left due to INTERNAL_SERVER_ERROR" in {
      server.stubFor(
        get(urlEqualTo(s"/find-my-nino-add-to-wallet/individuals/details/NINO/$nino/Y"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val eitherResult = actionHelper.checkForCrn(nino, sessionId, fakeAuthContext, messages).futureValue

      eitherResult mustBe a[Left[_, _]]

      val result = eitherResult.swap.getOrElse(Results.Status(IM_A_TEAPOT))
      result.header.status mustBe INTERNAL_SERVER_ERROR

      contentAsString(Future.successful(result)) must include("Sorry, the service is unavailable")
    }

    "return UserRequest when individualDetails are found but individualDetails.crnIndicator is FALSE" in {

      val individualDetailsJson = Json.toJson(fakeIndividualDetailsDataCache).toString()

      server.stubFor(
        get(urlEqualTo(s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"))
          .willReturn(okJson(individualDetailsJson))
      )

      val result = actionHelper.checkForCrn(nino, sessionId, fakeAuthContext, messages).futureValue

      result mustBe a[Right[_, _]]
      val userRequest = result.toOption.get
      userRequest.enrolments mustBe Enrolments(Set(Enrolment("HMRC-PT")))
    }

    "return RedirectToPostalFormView when individualDetails.crnIndicator is TRUE but crnUpliftEnabled flag is FALSE" in {
      val disabledFeatureApp: Application = new GuiceApplicationBuilder()
        .configure(
          "microservice.services.individual-details.port" -> server.port(),
          "microservice.services.individual-details.host" -> "127.0.0.1",
          "features.crn-uplift-enabled"                   -> false
        )
        .build()

      val actionHelperWithDisabledFeature: ActionHelper = disabledFeatureApp.injector.instanceOf[ActionHelper]

      server.stubFor(
        get(urlEqualTo(s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"))
          .willReturn(okJson(individualDetailsCrnTrueJson))
      )

      val eitherResult = actionHelperWithDisabledFeature
        .checkForCrn(nino, sessionId, fakeAuthContext, messages)
        .futureValue

      eitherResult mustBe a[Left[_, _]]

      val result = eitherResult.swap.getOrElse(Results.Status(IM_A_TEAPOT))
      result.header.status mustBe OK

      contentAsString(Future.successful(result)) must include("fill out a CA5403 form online")
    }

    "return UserRequest when CRN uplift succeeds and adult-registration API responds with NO_CONTENT" in {

      server.stubFor(
        get(urlEqualTo(s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"))
          .willReturn(okJson(individualDetailsCrnTrueJson))
      )

      server.stubFor(
        put(urlEqualTo(s"/find-my-nino-add-to-wallet/adult-registration/$nino"))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val result = actionHelper.checkForCrn(nino, sessionId, fakeAuthContext, messages).futureValue

      result mustBe a[Right[_, _]]
      val userRequest = result.toOption.get
      userRequest.enrolments mustBe Enrolments(Set(Enrolment("HMRC-PT")))
    }

//    "return 500 with TechnicalIssuesNoRetryView when CRN uplift fails because no name is available" in {
//
//      server.stubFor(
//        get(urlEqualTo(s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"))
//          .willReturn(
//            okJson(
//              Json
//                .toJson(
//                  fakeIndividualDetailsDataCache.copy(
//                    individualDetailsData = fakeIndividualDetailsDataCache.individualDetailsData.copy(
//                    crnIndicator = "true",
//                    nameList = NameList(List(fakeName.copy(firstForename = None, surname = None)))
//                  )
//                )
//                .toString()
//            )
//          )
//      )
//
//      val result = actionHelper.checkForCrn(nino, sessionId, fakeAuthContext, messages).futureValue
//
//      result mustBe a[Left[_, _]]
//      result.swap.getOrElse(Results.Ok("")).header.status mustBe INTERNAL_SERVER_ERROR
//    }

    "return UserRequest when CRN uplift succeeds and adult-registration API responds with UNPROCESSABLE_ENTITY but contains alreadyAnAdultErrorCode" in {

      val jsonUnprocessableEntityAlreadyAdult: String =
        s"""
           |{
           |  "failures": [
           |    {
           |      "reason": "Already an adult account",
           |      "code": "63492"
           |    }
           |  ]
           |}
           |""".stripMargin

      server.stubFor(
        get(urlEqualTo(s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"))
          .willReturn(okJson(individualDetailsCrnTrueJson))
      )

      server.stubFor(
        put(urlEqualTo(s"/find-my-nino-add-to-wallet/adult-registration/$nino"))
          .willReturn(aResponse().withStatus(UNPROCESSABLE_ENTITY).withBody(jsonUnprocessableEntityAlreadyAdult))
      )

      val result = actionHelper.checkForCrn(nino, sessionId, fakeAuthContext, messages).futureValue

      result mustBe a[Right[_, _]]
      val userRequest = result.toOption.get
      userRequest.enrolments mustBe Enrolments(Set(Enrolment("HMRC-PT")))
    }

    List(
      INTERNAL_SERVER_ERROR,
      UNPROCESSABLE_ENTITY,
      BAD_REQUEST,
      NOT_FOUND
    ).foreach { errorCode =>
      s"return 500 with TechnicalIssuesNoRetryView when upliftCRN (adult-registration) API fails with Left due to $errorCode" in {

        server.stubFor(
          get(urlEqualTo(s"/find-my-nino-add-to-wallet/individuals/details/NINO/${nino.take(8)}/Y"))
            .willReturn(okJson(individualDetailsCrnTrueJson))
        )

        server.stubFor(
          put(urlEqualTo(s"/find-my-nino-add-to-wallet/adult-registration/$nino"))
            .willReturn(aResponse().withStatus(errorCode))
        )

        val eitherResult = actionHelper.checkForCrn(nino, sessionId, fakeAuthContext, messages).futureValue

        eitherResult mustBe a[Left[_, _]]

        val result = eitherResult.swap.getOrElse(Results.Status(IM_A_TEAPOT))
        result.header.status mustBe INTERNAL_SERVER_ERROR

        contentAsString(Future.successful(result)) must include("Sorry, the service is unavailable")
      }
    }
  }
}
