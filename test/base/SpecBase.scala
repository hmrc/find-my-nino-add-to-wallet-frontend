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

package base

import config.FrontendAppConfig
import controllers.actions._
import models.UserAnswers
import org.jsoup.Jsoup
import org.scalactic.source.Position
import org.scalatest.Assertion
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.{Cell, RequestAttrKey}
import play.api.mvc.{Cookie, Cookies, MessagesControllerComponents, RequestHeader}
import play.api.test.FakeRequest
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.sca.connectors.ScaWrapperDataConnector
import uk.gov.hmrc.sca.models.{MenuItemConfig, PtaMinMenuConfig, WrapperDataResponse}
import uk.gov.hmrc.sca.utils.Keys
import util.WireMockSupport

import scala.reflect.ClassTag

class SpecBase extends WireMockSupport with MockitoSugar with GuiceOneAppPerSuite {

  implicit lazy val application: Application = applicationBuilder().build()
  implicit lazy val applicationWithConfig: Application = applicationBuilderWithConfig().build()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val userAnswersId: String = "id"

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())


  def messages(app: Application, request: RequestHeader): Messages =
    app.injector.instanceOf[MessagesApi].preferred(request)

  protected implicit def hc(implicit rh: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(rh, rh.session)

  protected def applicationBuilderWithConfig(
                                              config: Map[String, Any] = Map(),
                                              userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        config ++ Map(
          "microservice.services.auth.port" -> wiremockPort,
          "microservice.host" -> "http://localhost:9900/fmn"
        )
      )
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  protected def assertSameHtmlAfter(
                                     transformation: String => String
                                   )(left: String, right: String)(implicit position: Position): Assertion = {
    val leftHtml = Jsoup.parse(transformation(left))
    val rightHtml = Jsoup.parse(transformation(right))
    leftHtml.html() mustBe rightHtml.html()
  }

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)

  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  //implicit lazy val ec = app.injector.instanceOf[ExecutionContext]

  lazy val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val config = app.injector.instanceOf[Configuration]

  implicit lazy val cc = app.injector.instanceOf[MessagesControllerComponents]

  implicit lazy val env = app.injector.instanceOf[Environment]

  def buildFakeRequestWithSessionId(method: String) =
    FakeRequest(method, "/save-your-national-insurance-number").withSession("sessionId" -> "FAKE_SESSION_ID")

  val mockScaWrapperDataConnector: ScaWrapperDataConnector = mock[ScaWrapperDataConnector]

  val ptaMenuConfig: PtaMinMenuConfig = PtaMinMenuConfig(menuName = "Account menu", backName = "Back")
  val menuItemConfig1: MenuItemConfig = MenuItemConfig("home", "Account home", "pertaxUrl", leftAligned = true, position = 0, Some("hmrc-account-icon hmrc-account-icon--home"), None)
  val menuItemConfig2: MenuItemConfig = MenuItemConfig("messages", "Messages", "pertaxUrl-messages", leftAligned = false, position = 0, None, None)
  val menuItemConfig3: MenuItemConfig = MenuItemConfig("progress", "Check progress", "trackingUrl-track", leftAligned = false, position = 1, None, None)
  val menuItemConfig4: MenuItemConfig = MenuItemConfig("profile", "Profile and settings", "pertaxUrl-profile-and-settings", leftAligned = false, position = 2, None, None)
  val menuItemConfig5: MenuItemConfig = MenuItemConfig("signout", "Sign out", "pertaxUrl-signout-feedback-PERTAX", leftAligned = false, position = 3, None, None)
  val wrapperDataResponse: WrapperDataResponse = WrapperDataResponse(Seq(menuItemConfig1, menuItemConfig2, menuItemConfig3, menuItemConfig4, menuItemConfig5), ptaMenuConfig)



  val messageDataResponse: Option[Int] = Some(2)

  val requestAttributeMap: TypedMap = TypedMap(
    Keys.wrapperDataKey -> wrapperDataResponse,
    Keys.messageDataKey -> messageDataResponse,
    RequestAttrKey.Cookies -> Cell(Cookies(Seq(Cookie("PLAY_LANG", "en")))))
}


