

package controllers.auth

import config.FrontendAppConfig
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.Configuration
import play.api.mvc.MessagesControllerComponents
import play.api.test._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.{FrontendBaseController, FrontendHeaderCarrierProvider}

import scala.concurrent.Future

class FMNAuthSpec extends PlaySpec with MockitoSugar {

  "FMNAuth" should {

    "call the upliftConfidenceLevel method when confidence level is less than 200" in {
      // Create a mock instance of AuthConnector and FrontendAppConfig
      val mockAuthConnector = mock[AuthConnector]
      val mockFrontendAppConfig = mock[FrontendAppConfig]
      val mockConfiguration = mock[Configuration]
      val mockMessagesControllerComponents = mock[MessagesControllerComponents]
      // Set up the AuthConnector to return a ConfidenceLevel less than 200 and an AffinityGroup of Individual
      when(mockAuthConnector.authorise(any, any)(any, any))
        .thenReturn(Future.successful(new ~(Some(ConfidenceLevel.L50), Some(AffinityGroup.Individual))))

      // Create an instance of FMNAuth
      val fmnAuth = new FMNAuth {
        def authConnector: AuthConnector = mockAuthConnector
        def config: Configuration = mockConfiguration
        def frontendAppConfig: FrontendAppConfig = mockFrontendAppConfig
        def controllerComponents: MessagesControllerComponents = mockMessagesControllerComponents
      }

      // Call the authorisedAsFMNUser method
      val result = fmnAuth.
      // Verify that the upliftConfidenceLevel method was called
      verify(fmnAuth).upliftConfidenceLevel(any())
    }
  }
}