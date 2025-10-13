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

package util

import controllers.auth.requests.UserRequest
import models.individualDetails.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.*
import play.api.test.{FakeRequest, Helpers, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Generator, Nino, SaUtrGenerator}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.partials.FormPartialRetriever

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait IndividualDetailsFixtures {

  trait BaseSpec
      extends AnyWordSpec
      with GuiceOneAppPerSuite
      with Matchers
      with PatienceConfiguration
      with BeforeAndAfterEach
      with MockitoSugar
      with ScalaFutures
      with Injecting {
    this: Suite =>

    implicit val hc: HeaderCarrier    = HeaderCarrier()
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    val mockPartialRetriever: FormPartialRetriever = mock[FormPartialRetriever]
    when(mockPartialRetriever.getPartialContentAsync(any(), any(), any())(any(), any())) thenReturn Future(Html(""))

    val configValues: Map[String, Any] =
      Map(
        "cookie.encryption.key"         -> "gvBoGdgzqG1AarzF1LY0zQ==",
        "sso.encryption.key"            -> "gvBoGdgzqG1AarzF1LY0zQ==",
        "queryParameter.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ==",
        "metrics.enabled"               -> false,
        "auditing.enabled"              -> false
      )

    protected def localGuiceApplicationBuilder(): GuiceApplicationBuilder =
      GuiceApplicationBuilder()
        .overrides(
          bind[FormPartialRetriever].toInstance(mockPartialRetriever)
        )
        .configure(configValues)

    override implicit lazy val app: Application = localGuiceApplicationBuilder().build()

  }
  trait ActionBuilderFixture extends ActionBuilder[UserRequest, AnyContent] {
    override def invokeBlock[A](a: Request[A], block: UserRequest[A] => Future[Result]): Future[Result]
    override def parser: BodyParser[AnyContent]               = Helpers.stubBodyParser()
    override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }
}

object Fixtures extends IndividualDetailsFixtures {

  val fakeNino = Nino(new Generator(new Random()).nextNino.nino)

  val saUtr = new SaUtrGenerator().nextSaUtr

  val etag = "1"

  def buildFakeRequestWithSessionId(method: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, "/personal-account").withSession("sessionId" -> "FAKE_SESSION_ID")

  def buildFakeRequestWithAuth(
    method: String,
    uri: String = "/find-my-nino-add-to-wallet"
  ): FakeRequest[AnyContentAsEmpty.type] = {
    val session = Map(
      SessionKeys.sessionId            -> s"session-${UUID.randomUUID()}",
      SessionKeys.lastRequestTimestamp -> LocalDate.now().toEpochDay.toString
    )

    FakeRequest(method, uri).withSession(session.toList: _*)
  }

  val fakeAddressData: AddressData = AddressData(
    addressLine1 = AddressLine("123 Fake Street"),
    addressLine2 = Some(AddressLine("Apt 4B")),
    addressLine3 = Some(AddressLine("Faketown")),
    addressLine4 = Some(AddressLine("Fakeshire")),
    addressLine5 = Some(AddressLine("Fakecountry")),
    addressPostcode = Some(AddressPostcode("AA1 1AA")),
    addressCountry = "GREAT BRITAIN",
    addressStartDate = LocalDate.now(),
    addressType = AddressType.ResidentialAddress
  )

  val fakeIndividualDetails: IndividualDetails = IndividualDetails(
    title = Some("Dr"),
    firstForename = Some("Firstname"),
    secondForename = Some("Middlename"),
    surname = Some("Lastname"),
    honours = Some("Phd."),
    dateOfBirth = LocalDate.now(),
    nino = "AB123456C",
    address = Some(fakeAddressData),
    crnIndicator = "false"
  )

  val fakeindividualDetailsNoAddress: IndividualDetails = fakeIndividualDetails.copy(address = None)

  val fakeindividualDetailsWithCRN: IndividualDetails = fakeIndividualDetails.copy(crnIndicator = "true")

}
