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
import models.individualDetails._
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
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Generator, Nino, SaUtrGenerator}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.partials.FormPartialRetriever

import java.time.{Instant, LocalDate}
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

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockPartialRetriever = mock[FormPartialRetriever]
  when(mockPartialRetriever.getPartialContentAsync(any(), any(), any())(any(), any())) thenReturn Future(Html(""))


  val configValues: Map[String, Any] =
    Map(
      "cookie.encryption.key"         -> "gvBoGdgzqG1AarzF1LY0zQ==",
      "sso.encryption.key"            -> "gvBoGdgzqG1AarzF1LY0zQ==",
      "queryParameter.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ==",
      "json.encryption.key"           -> "gvBoGdgzqG1AarzF1LY0zQ==",
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


object Fixtures extends IndividualDetailsFixtures  {

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

  val individualRespJson =
    """
      |{
      |    "nino": "AA000003",
      |    "ninoSuffix": "B",
      |    "names": {
      |        "1": {
      |            "sequenceNumber": 3,
      |            "title": 2,
      |            "firstForenameOrInitial": "BOB",
      |            "surname": "JONES",
      |            "startDate": "2016-04-06"
      |        }
      |    },
      |    "sex": "F",
      |    "dateOfBirth": "1962-09-08",
      |    "dateOfBirthStatus": 0,
      |    "deceased": false,
      |    "dateOfDeathStatus": 0,
      |    "addresses": {
      |        "1": {
      |            "sequenceNumber": 3,
      |            "countryCode": 1,
      |            "line1": "11 Test Street",
      |            "line2": "Testtown",
      |            "postcode": "FX97 4TU",
      |            "startDate": "2016-04-06",
      |            "lastConfirmedDate": "2002-07-08"
      |        },
      |        "2": {
      |            "sequenceNumber": 3,
      |            "countryCode": 1,
      |            "line1": "11 Test Street",
      |            "line2": "Testtown",
      |            "postcode": "FX97 4TU",
      |            "startDate": "2012-07-05",
      |            "lastConfirmedDate": "2012-07-08"
      |        }
      |    },
      |    "phoneNumbers": {
      |        "3": {
      |            "telephoneType": 3,
      |            "telephoneNumber": "3984425669.02115"
      |        }
      |    },
      |    "accountStatus": 0,
      |    "manualCorrespondenceInd": false,
      |    "dateOfEntry": "1978-09-08",
      |    "hasSelfAssessmentAccount": false,
      |    "utr": "1097133333",
      |    "audioOutputRequired": false,
      |    "brailleOutputRequired": false,
      |    "largePrintOutputRequired": false,
      |    "welshOutputRequired": true
      |}
      |""".stripMargin

  val individualRespJsonInvalid: String =
    """
      |{
      |    "nino": "AA000003",
      |    "ninoSuffix": "B",
      |    "names": {
      |        "1": {
      |            "sequenceNumber": 3,
      |            "title": 2,
      |            "firstForenameOrInitial": "BOB",
      |            "startDate": "2016-04-06"
      |        }
      |    },
      |    "sex": "F",
      |    "dateOfBirth": "1962-09-08",
      |    "dateOfBirthStatus": 0,
      |    "deceased": false,
      |    "dateOfDeathStatus": 0,
      |    "addresses": {
      |        "1": {
      |            "sequenceNumber": 3,
      |            "countryCode": 1,
      |            "line1": "11 Test Street",
      |            "line2": "Testtown",
      |            "postcode": "FX97 4TU",
      |            "startDate": "2016-04-06",
      |            "lastConfirmedDate": "2002-07-08"
      |        },
      |        "2": {
      |            "sequenceNumber": 3,
      |            "countryCode": 1,
      |            "line1": "11 Test Street",
      |            "line2": "Testtown",
      |            "postcode": "FX97 4TU",
      |            "startDate": "2012-07-05",
      |            "lastConfirmedDate": "2012-07-08"
      |        }
      |    },
      |    "phoneNumbers": {
      |        "3": {
      |            "telephoneType": 3,
      |            "telephoneNumber": "3984425669.02115"
      |        }
      |    },
      |    "accountStatus": 0,
      |    "manualCorrespondenceInd": false,
      |    "dateOfEntry": "1978-09-08",
      |    "hasSelfAssessmentAccount": false,
      |    "utr": "1097133333",
      |    "audioOutputRequired": false,
      |    "brailleOutputRequired": false,
      |    "largePrintOutputRequired": false,
      |    "welshOutputRequired": true
      |}
      |""".stripMargin

  val fakeName: Name = models.individualDetails.Name(
    nameSequenceNumber = NameSequenceNumber(1),
    nameType = NameType.RealName,
    titleType = Some(TitleType.Dr),
    requestedName = Some(RequestedName("Firstname Middlename")),
    nameStartDate = NameStartDate(LocalDate.of(2000, 1, 1)),
    nameEndDate = Some(NameEndDate(LocalDate.of(2022, 12, 31))),
    otherTitle = Some(OtherTitle("Sir")),
    honours = Some(Honours("PhD")),
    firstForename = FirstForename("Firstname"),
    secondForename = Some(SecondForename("Middlename")),
    surname = Surname("Lastname")
  )

  val fakeKnownAsName: Name = models.individualDetails.Name(
    nameSequenceNumber = NameSequenceNumber(2),
    nameType = NameType.KnownAsName,
    titleType = Some(TitleType.Dr),
    requestedName = Some(RequestedName("Known As Name")),
    nameStartDate = NameStartDate(LocalDate.of(2000, 1, 1)),
    nameEndDate = Some(NameEndDate(LocalDate.of(2022, 12, 31))),
    otherTitle = Some(OtherTitle("Sir")),
    honours = Some(Honours("PhD")),
    firstForename = FirstForename("Known"),
    secondForename = Some(SecondForename("As")),
    surname = Surname("Name")
  )

  val fakeNameWithoutMiddleName = fakeName.copy(secondForename = None)

  val fakeAddress: Address = Address(
    addressSequenceNumber = AddressSequenceNumber(0),
    addressSource = Some(AddressSource.Customer),
    countryCode = CountryCode(826),
    addressType = AddressType.ResidentialAddress,
    addressStatus = Some(AddressStatus.NotDlo),
    addressStartDate = LocalDate.of(2000, 1, 1),
    addressEndDate = Some(LocalDate.of(2022, 12, 31)),
    addressLastConfirmedDate = Some(LocalDate.of(2022, 1, 1)),
    vpaMail = Some(VpaMail(1)),
    deliveryInfo = Some(DeliveryInfo("Delivery info")),
    pafReference = Some(PafReference("PAF reference")),
    addressLine1 = AddressLine("123 Fake Street"),
    addressLine2 = AddressLine("Apt 4B"),
    addressLine3 = Some(AddressLine("Faketown")),
    addressLine4 = Some(AddressLine("Fakeshire")),
    addressLine5 = Some(AddressLine("Fakecountry")),
    addressPostcode = Some(AddressPostcode("AA1 1AA"))
  )

  val fakeAddressData: AddressData = AddressData(
    addressLine1 = AddressLine("123 Fake Street"),
    addressLine2 = AddressLine("Apt 4B"),
    addressLine3 = Some(AddressLine("Faketown")),
    addressLine4 = Some(AddressLine("Fakeshire")),
    addressLine5 = Some(AddressLine("Fakecountry")),
    addressPostcode = Some(AddressPostcode("AA1 1AA")),
    addressCountry = "GREAT BRITAIN",
    addressStartDate = LocalDate.now(),
    addressType = AddressType.ResidentialAddress
  )

  val fakeIndividualDetails: IndividualDetails = IndividualDetails(
    ninoWithoutSuffix = "AB123456",
    ninoSuffix = Some(NinoSuffix("C")),
    accountStatusType = Some(AccountStatusType.FullLive),
    dateOfEntry = Some(LocalDate.of(2000, 1, 1)),
    dateOfBirth = LocalDate.of(1990, 1, 1),
    dateOfBirthStatus = Some(DateOfBirthStatus.Verified),
    dateOfDeath = None,
    dateOfDeathStatus = None,
    dateOfRegistration = Some(LocalDate.of(2000, 1, 1)),
    crnIndicator = CrnIndicator.False,
    nameList = NameList(List(fakeName)),
    addressList = AddressList(Some(List(fakeAddress)))
  )

  val fakeIndividualDetailsWithKnownAsName = fakeIndividualDetails.copy(nameList = NameList(List(fakeName, fakeKnownAsName)))

  val fakeIndividualDetailsWithoutMiddleName = fakeIndividualDetails.copy(nameList = NameList(List(fakeNameWithoutMiddleName)))

  val fakeIndividualDetailsData = IndividualDetailsData(
    fullName = "Dr Firstname Middlename Lastname Phd.",
    firstForename = "Firstname",
    surname = "Lastname",
    initialsName = "FML",
    dateOfBirth = LocalDate.now(),
    nino = "AB123456C",
    address = Some(fakeAddressData),
    crnIndicator = "false"
  )

  val fakeIndividualDetailsDataNoAddress = fakeIndividualDetailsData.copy(address = None)

  val fakeIndividualDetailsDataCache = IndividualDetailsDataCache(
    "some-fake-Id",
    fakeIndividualDetailsData,
    Instant.now(java.time.Clock.systemUTC())
  )

  val fakeIndividualDetailsDataCacheNoAddress = IndividualDetailsDataCache(
    "some-fake-Id",
    fakeIndividualDetailsDataNoAddress,
    Instant.now(java.time.Clock.systemUTC())
  )

  val fakeIndividualDetailsDataWithCRN = fakeIndividualDetailsData.copy(crnIndicator = "true")

  val fakeIndividualDetailsDataCacheWithCRN = fakeIndividualDetailsDataCache.copy(individualDetailsData = fakeIndividualDetailsDataWithCRN)

  val fakeIndividualDetailsDataCacheMissingNinoSuffix = fakeIndividualDetailsDataCache
    .copy(individualDetailsData = fakeIndividualDetailsData
      .copy(nino = "AB123456"))
}