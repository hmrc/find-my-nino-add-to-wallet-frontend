package base

import base.WiremockHelper.wiremockPort
import config.FrontendAppConfig
import constants.BaseITConstants
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.ExecutionContext

trait IntegrationSpecBase extends PlaySpec
  with GivenWhenThen with TestSuite with ScalaFutures with IntegrationPatience
  with WiremockHelper
  with GuiceOneServerPerSuite with TryValues
  with BeforeAndAfterEach with BeforeAndAfterAll with Eventually with CreateRequestHelper with CustomMatchers with BaseITConstants with DefaultAwaitTimeout {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  def config: Map[String, _] = Map(
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "play.http.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "auditing.enabled" -> false,
    "metrics.enabled" -> false,
    "microservice.services.auth.port" -> wiremockPort,
    "microservice.services.history.port" -> wiremockPort,
    "microservice.services.exbForms.port" -> wiremockPort
  )

  implicit lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val appConfig = app.injector.instanceOf[FrontendAppConfig]
  implicit lazy val fakeRequest = FakeRequest("", "").withSession(SessionKeys.sessionId -> "foo")

  override def beforeEach(): Unit = {
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    stopWiremock()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

}
