import base.IntegrationSpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, urlEqualTo}
import connectors.{CitizenDetailsConnector, PersonDetailsNotFoundResponse, PersonDetailsSuccessResponse}
import models.{Address, Person, PersonDetails}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

class CitizenDetailsConnectorISpec extends IntegrationSpecBase {
  private val stubNino = Nino("SK604414C")
  private val happyDesignatoryDetailsJson =
    s"""{
       |"{
       |    "person": {
       |        "firstName": "ANNE",
       |        "lastName": "CASSIDY",
       |        "title": "Mrs",
       |        "sex": "F",
       |        "dateOfBirth": "1960-05-05",
       |        "nino": "$stubNino",
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
      None,
      Some("Mrs"),
      None,
      Some("F"),
      Some(LocalDate.parse("1960-05-05")),
      Some(stubNino)
    ),
    Some(
      Address(
        Some("ST. LEONARDS HALL"),
        Some("WOODSTON"),
        None,
        None,
        None,
        Some("BH8 8LP"),
        Some("GREAT BRITAIN"),
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

        wireMockServer.stubFor(get(urlEqualTo(s"/citizen-details/$stubNino/designatory-details"))
          .willReturn(ok(happyDesignatoryDetailsJson)))

        val result = connector.personDetails(stubNino).futureValue

        result mustBe PersonDetailsSuccessResponse(happyDesignatoryDetailsModel)

      }
    }

      "when an invalid nino is given" must {

        "return a not found response" in {

          wireMockServer.stubFor(
            get(urlEqualTo(s"/citizen-details/$generatedNino/designatory-details"))
              .willReturn(aResponse().withStatus(404))
          )

          val result = connector.personDetails(generatedNino)

          result.futureValue mustBe PersonDetailsNotFoundResponse
        }
      }
    }
}
