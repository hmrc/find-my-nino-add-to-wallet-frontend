import base.IntegrationSpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import connectors.CitizenDetailsConnector
import models.{Address, Person, PersonDetails}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

class CitizenDetailsConnectorISpec extends IntegrationSpecBase {
  private val testNino = Nino("SK604414C")
  private val happyDesignatoryDetailsJson =
    s"""{
       |"{
       |    "person": {
       |        "firstName": "ANNE",
       |        "lastName": "CASSIDY",
       |        "title": "Mrs",
       |        "sex": "F",
       |        "dateOfBirth": "1960-05-05",
       |        "nino": "$testNino",
       |        "deceased": false
       |    },
       |    "address": {
       |        "line1": "ST. LEONARDS HALL",
       |        "line2": "WOODSTON",
       |        "postcode": "BH8 8LP",
       |        "startDate": "2009-11-10",
       |        "country": "GREAT BRITAIN",
       |        "status": 0,
       |        "type": "Residential"
       |    }
       |}""".stripMargin

  private val happyDesignatoryDetailsModel = PersonDetails(
    Person(
      Some("ANNE"),
      None,
      Some("CASSIDY"),
      Some("AC"),
      Some("Mrs"),
      None,
      Some("F"),
      Some(LocalDate.parse("1960-05-05")),
      Some(testNino)
    ),
    Some(
      Address(
        Some("ST. LEONARDS HALL"),
        Some("WOODSTON"),
        Some("BH8 8LP"),
        None,
        None,
        Some("AA1 1AA"),
        None,
        Some(LocalDate.of(2009, 11, 10)),
        None,
        Some("Residential"),
        isRls = false
      )
    ),
    None
  )

  private lazy val connector: CitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]

  "GET /citizen-details/$nino/designatory-details" must {

    "when a valid json is returned" must {

      "return the valid PersonDetails model" in {
        wireMockServer.stubFor(get(urlEqualTo(s"/citizen-details/$testNino/designatory-details"))
          .willReturn(ok(happyDesignatoryDetailsJson)))

        val result = connector.personDetails(testNino).futureValue

        result mustEqual happyDesignatoryDetailsModel

      }

      "when an error is returned" must {

        "return a failed future" in {

          wireMockServer.stubFor(
            get(urlEqualTo(s"/citizen-details/$testNino/designatory-details"))
              .willReturn(aResponse().withStatus(500))
          )

          val result = connector.personDetails(testNino)

          result.futureValue mustBe 500
        }
      }
    }
  }
}
