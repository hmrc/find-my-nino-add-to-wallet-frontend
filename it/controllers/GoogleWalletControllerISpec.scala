
package controllers

import base.IntegrationSpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import config.ConfigDecorator
import controllers.auth.requests.UserRequest
import controllers.auth.routes
import models.{Address, Person, PersonDetails, UserName}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.GoogleWalletView

import java.time.LocalDate

class GoogleWalletControllerISpec extends IntegrationSpecBase {

  //generate fake details to provide for testing the functionality of the google wallet page
  val fakePersonDetails: PersonDetails = PersonDetails(
    Person(
      Some("John"),
      None,
      Some("Doe"),
      Some("JD"),
      Some("Mr"),
      None,
      Some("M"),
      Some(LocalDate.parse("1975-12-03")),
      Some(generatedNino)
    ),
    Some(
      Address(
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
        isRls = false
      )
    ),
    None
  )

  val fakePassId = "googlePassId"
  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"


  override lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]


  implicit lazy val configDecorator: ConfigDecorator = app.injector.instanceOf[ConfigDecorator]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi).messages

  trait LocalSetup {
    def buildUserRequest[A](
                             nino: Option[Nino] = Some(generatedNino),
                             userName: Option[UserName] = Some(UserName(Name(Some("Firstname"), Some("Lastname")))),
                             confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
                             personDetails: Option[PersonDetails] = Some(fakePersonDetails),
                             request: Request[A] = FakeRequest().asInstanceOf[Request[A]]
                           ): UserRequest[A] =
      UserRequest(
        nino,
        userName,
        confidenceLevel,
        personDetails,
        Enrolments(Set(Enrolment("HMRC-PT"))),
        request
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

    def assertContainsQRCode(doc: Document, text: String): Unit = {
      assert(
        doc.getElementById("google-qr-code").attr("src").contains(text),
        s"\n\nQR Code linking to " + text + " was not rendered on the page. \n"
      )
    }
  }

  "Main" when {

    "rendering the view" must {
      "render the welsh language toggle" in new LocalSetup {
        assertContainsLink(doc, "Cymraeg", "/hmrc-frontend/language/cy")
      }

      "render the google pass QR code on desktop" in new LocalSetup {
        wireMockServer.stubFor(get(s"${configDecorator.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-google-pass?passId=$fakePassId").willReturn(ok(fakeBase64String)))
        assertContainsQRCode(doc, s"/get-google-qr-code?passId=$fakePassId")
      }

      "render the save to Google Wallet link on mobile" in new LocalSetup {
        wireMockServer.stubFor(get(s"${configDecorator.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-pass-card?passId=$fakePassId").willReturn(ok(fakeBase64String)))
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
        assertContainsLink(doc, "Profile and settings", "/personal-account/your-profile")
      }

      "render the sign out link" in new LocalSetup {

        val href: String =  routes.AuthController
          .signout(Some(RedirectUrl(configDecorator.getFeedbackSurveyUrl(configDecorator.defaultOrigin))), None)
          .url

        assertContainsLink(doc, "Sign Out", href)
      }
    }
  }
}
