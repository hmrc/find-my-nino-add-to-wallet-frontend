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

package controllers

import base.IntegrationSpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import config.FrontendAppConfig
import controllers.auth.requests.UserRequest
import controllers.auth.routes
import models.individualDetails.IndividualDetailsDataCache
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import util.Fixtures
import views.html.GoogleWalletView

import java.net.URLDecoder

class GoogleWalletControllerISpec extends IntegrationSpecBase {

  val fakePassId       = "googlePassId"
  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"

  override lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val frontendAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  implicit lazy val messages: Messages                   = MessagesImpl(Lang("en"), messagesApi).messages

  trait LocalSetup {
    def buildUserRequest[A](
      nino: Option[Nino] = Some(generatedNino),
      confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
      individualDetailsData: IndividualDetailsDataCache = Fixtures.fakeIndividualDetailsDataCache,
      request: Request[A] = FakeRequest().asInstanceOf[Request[A]]
    ): UserRequest[A] =
      UserRequest(
        nino,
        confidenceLevel,
        individualDetailsData,
        Enrolments(Set(Enrolment("HMRC-PT"))),
        request,
        None
      )

    implicit val userRequest: UserRequest[AnyContentAsEmpty.type] = buildUserRequest()

    def view: GoogleWalletView = app.injector.instanceOf[GoogleWalletView]

    def main: Html =
      view(
        googlePassId = fakePassId,
        displayForMobile = false
      )(fakeRequest, messages)

    def doc: Document = Jsoup.parse(main.toString)

    def mainMobile: Html =
      view(
        googlePassId = fakePassId,
        displayForMobile = true
      )(fakeRequest, messages)

    def docMobile: Document = Jsoup.parse(mainMobile.toString)

    def assertContainsText(doc: Document, text: String): Assertion =
      assert(doc.toString.contains(text), "\n\ntext " + text + " was not rendered on the page.\n")

    def assertContainsLink(doc: Document, text: String, href: String): Assertion =
      assert(
        doc.getElementsContainingText(text).attr("href").contains(href),
        s"\n\nLink $href was not rendered on the page\n"
      )

    def assertContainsLinkByContainingClass(doc: Document, href: String): Assertion =
      assert(
        doc.getElementsByClass("show-on-phones").select("a[href]").attr("href").contains(href),
        s"\n\nLink $href was not rendered on the page\n"
      )

    def assertContainsQRCode(doc: Document, text: String): Unit =
      assert(
        doc.getElementById("google-qr-code").attr("src").contains(text),
        s"\n\nQR Code linking to " + text + " was not rendered on the page. \n"
      )
  }

  "Main" when {

    "rendering the view" must {
      "render the welsh language toggle" in new LocalSetup {
        assertContainsLink(doc, "Cymraeg", "/hmrc-frontend/language/cy")
      }

      "render the google pass QR code on desktop" in new LocalSetup {
        wireMockServer.stubFor(
          get(
            s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-google-pass?passId=$fakePassId"
          ).willReturn(ok(fakeBase64String))
        )
        assertContainsQRCode(doc, s"/get-google-qr-code?passId=$fakePassId")
      }

      "render the save to Google Wallet link on mobile" in new LocalSetup {

        wireMockServer.stubFor(
          get(s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-pass-card?passId=$fakePassId")
            .willReturn(ok(fakeBase64String))
        )
        assertContainsLinkByContainingClass(docMobile, s"/get-google-pass?passId=$fakePassId")
      }
    }

    "rendering the nav bar" must {

      "render the Account home button" in new LocalSetup {
        assertContainsLink(doc, "Account Home", "/personal-account")
      }

      "render the Messages link" in new LocalSetup {
        assertContainsLink(doc, "Messages", "/personal-account/messages")
      }

      "render the Check progress link" in new LocalSetup {
        assertContainsLink(doc, "Check progress", "/track")
      }

      "render the Your Profile link" in new LocalSetup {
        assertContainsLink(doc, "Profile and settings", "/personal-account/profile-and-settings")
      }

      "render the sign out link" in new LocalSetup {

        val href: String = routes.AuthController
          .signout(Some(RedirectUrl(frontendAppConfig.getFeedbackSurveyUrl(frontendAppConfig.defaultOrigin))), None)
          .url

        assertContainsLink(doc, "Sign Out", URLDecoder.decode(href, "UTF-8"))
      }
    }
  }
}
