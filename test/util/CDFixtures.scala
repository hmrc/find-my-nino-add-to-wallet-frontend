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
import models._
import models.individualDetails.{AccountStatusType, Address, AddressLine, AddressList, AddressPostcode, AddressSequenceNumber, AddressSource, AddressStatus, AddressType, CountryCode, CrnIndicator, DateOfBirthStatus, DeliveryInfo, FirstForename, Honours, IndividualDetails, Name, NameEndDate, NameList, NameSequenceNumber, NameStartDate, NameType, NinoSuffix, OtherTitle, PafReference, RequestedName, SecondForename, Surname, TitleType, VpaMail}
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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Generator, Nino, SaUtrGenerator}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.partials.FormPartialRetriever

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

trait CDFixtures {
  def buildPersonDetails: PersonDetails =
    PersonDetails(
      Person(
        Some("Firstname"),
        Some("Middlename"),
        Some("Lastname"),
        Some("FML"),
        Some("Dr"),
        Some("Phd."),
        Some("M"),
        Some(LocalDate.parse("1945-03-18")),
        Some(Fixtures.fakeNino)
      ),
      Some(buildFakeAddress),
      None
    )

  def buildPersonDetailsCorrespondenceAddress: PersonDetails =
    PersonDetails(
      Person(
        Some("Firstname"),
        Some("Middlename"),
        Some("Lastname"),
        Some("FML"),
        Some("Dr"),
        Some("Phd."),
        Some("M"),
        Some(LocalDate.parse("1945-03-18")),
        Some(Fixtures.fakeNino)
      ),
      Some(buildFakeAddress),
      Some(buildFakeCorrespondenceAddress)
    )

  def buildPersonDetailsWithoutAddress: PersonDetails =
    PersonDetails(
      Person(
        Some("Firstname"),
        Some("Middlename"),
        Some("Lastname"),
        Some("FML"),
        Some("Dr"),
        Some("Phd."),
        Some("M"),
        Some(LocalDate.parse("1945-03-18")),
        Some(Fixtures.fakeNino)
      ),
      None,
      Some(buildFakeCorrespondenceAddress)
    )

  def buildPersonDetailsWithoutCorrespondenceAddress: PersonDetails =
    PersonDetails(
      Person(
        Some("Firstname"),
        Some("Middlename"),
        Some("Lastname"),
        Some("FML"),
        Some("Dr"),
        Some("Phd."),
        Some("M"),
        Some(LocalDate.parse("1945-03-18")),
        Some(Fixtures.fakeNino)
      ),
      Some(buildFakeAddress),
      None
    )

  def buildFakeAddress: models.Address = models.Address(
    Some("1 Fake Street"),
    Some("Fake Town"),
    Some("Fake City"),
    Some("Fake Region"),
    None,
    Some("AA1 1AA"),
    None,
    Some(LocalDate.of(2015, 3, 15)),
    None,
    Some("Residential"),
    false
  )

  def buildFakeCorrespondenceAddress: models.Address = models.Address(
    Some("2 Fake Street"),
    Some("Fake Town"),
    Some("Fake City"),
    Some("Fake Region"),
    None,
    Some("AA1 1AA"),
    None,
    Some(LocalDate.of(2015, 3, 15)),
    None,
    Some("Correspondence"),
    false
  )

  def buildFakeAddressWithEndDate: models.Address = models.Address(
    Some("1 Fake Street"),
    Some("Fake Town"),
    Some("Fake City"),
    Some("Fake Region"),
    None,
    Some("AA1 1AA"),
    None,
    Some(LocalDate.now),
    Some(LocalDate.now),
    Some("Correspondence"),
    false
  )

  def buildFakeJsonAddress: JsValue = Json.toJson(buildFakeAddress)



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

  implicit val hc = HeaderCarrier()

  val mockPartialRetriever = mock[FormPartialRetriever]
  when(mockPartialRetriever.getPartialContent(any(), any(), any())(any(), any())) thenReturn Html("")


  val configValues =
    Map(
      "cookie.encryption.key"         -> "gvBoGdgzqG1AarzF1LY0zQ==",
      "sso.encryption.key"            -> "gvBoGdgzqG1AarzF1LY0zQ==",
      "queryParameter.encryption.key" -> "gvBoGdgzqG1AarzF1LY0zQ==",
      "json.encryption.key"           -> "gvBoGdgzqG1AarzF1LY0zQ==",
      "metrics.enabled"               -> false,
      "auditing.enabled"              -> false
    )

  protected def localGuiceApplicationBuilder(
    personDetails: Option[PersonDetails] = None
  ): GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .overrides(
        bind[FormPartialRetriever].toInstance(mockPartialRetriever),
//         bind[AuthJourney].toInstance(new FakeAuthJourney(saUser, personDetails))
      )
      .configure(configValues)

  override implicit lazy val app: Application = localGuiceApplicationBuilder().build()

  //implicit lazy val ec = app.injector.instanceOf[ExecutionContext]

  //lazy val config = app.injector.instanceOf[ConfigDecorator]

  //def injected[T](c: Class[T]): T = app.injector.instanceOf(c)

  //def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

}
trait ActionBuilderFixture extends ActionBuilder[UserRequest, AnyContent] {
  override def invokeBlock[A](a: Request[A], block: UserRequest[A] => Future[Result]): Future[Result]
  override def parser: BodyParser[AnyContent]               = Helpers.stubBodyParser()
  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
}


object Fixtures extends CDFixtures  {

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

  /*def buildFakePersonDetails: PersonDetails = PersonDetails(buildFakePerson, None, None)

  def buildFakePerson: Person =
    Person(
      Some("Firstname"),
      Some("Middlename"),
      Some("Lastname"),
      Some("FML"),
      Some("Mr"),
      None,
      Some("M"),
      Some(LocalDate.parse("1931-01-17")),
      Some(Fixtures.fakeNino)
    )*/

  val fakeName: Name = models.individualDetails.Name(
    nameSequenceNumber = NameSequenceNumber(1),
    nameType = NameType.RealName,
    titleType = Some(TitleType.Mr),
    requestedName = Some(RequestedName("John Doe")),
    nameStartDate = NameStartDate(LocalDate.of(2000, 1, 1)),
    nameEndDate = Some(NameEndDate(LocalDate.of(2022, 12, 31))),
    otherTitle = Some(OtherTitle("Sir")),
    honours = Some(Honours("PhD")),
    firstForename = FirstForename("John"),
    secondForename = Some(SecondForename("Doe")),
    surname = Surname("Smith")
  )

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
    nameList = NameList(Some(List(fakeName))),
    addressList = AddressList(Some(List(fakeAddress)))
  )

}
