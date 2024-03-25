
package connectors
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.await
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import util.WireMockHelper

import scala.concurrent.Future
class IndividualDetailsConnectorSpec extends ConnectorSpec
  with WireMockHelper
  with MockitoSugar
  with DefaultAwaitTimeout
  with Injecting {

    "IndividualDetailsConnector" should {

      "return the expected result from getIndividualDetails" in {

        val mockHttpClient = mock[HttpClient]
        val mockConfig = mock[FrontendAppConfig]
        val connector = new IndividualDetailsConnector(mockHttpClient, mockConfig)
        val nino = "AB123456C"
        val resolveMerge = "Y"
        val expectedResponse = HttpResponse(OK, "response body")

        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(mockHttpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(expectedResponse))

        val result = await(connector.getIndividualDetails(nino, resolveMerge))

        result mustBe expectedResponse

      }

  }
}