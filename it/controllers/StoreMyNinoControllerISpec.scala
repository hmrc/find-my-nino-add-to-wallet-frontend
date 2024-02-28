
package controllers

import base.IntegrationSpecBase
import config.FrontendAppConfig
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
import views.html.StoreMyNinoView

import java.net.URLDecoder
import java.time.LocalDate

class StoreMyNinoControllerISpec extends IntegrationSpecBase {

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

  val fakeGooglePassId = "googlePassId"
  val fakeApplePassId = "applePassId"

  val fakeBase64String = "UEsDBBQACAgIABxqJlYAAAAAAA"

  override lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val frontendAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi).messages

  trait LocalSetup {
    def buildUserRequest[A](
                             nino: Option[Nino] = Some(generatedNino),
                             userName: Option[UserName] = Some(UserName(Name(Some("Firstname"), Some("Lastname")))),
                             confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
                             personDetails: PersonDetails = fakePersonDetails,
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

    def view: StoreMyNinoView = app.injector.instanceOf[StoreMyNinoView]

    def main: Html =
      view(
        applePassId = fakeApplePassId,
        googlePassId = fakeGooglePassId,
        nino = generatedNino.nino,
        displayForMobile = true
      )(fakeRequest, messages)

    def doc: Document = Jsoup.parse(main.toString)

    def assertContainsText(doc: Document, text: String): Assertion =
      assert(doc.toString.contains(text), "\n\ntext " + text + " was not rendered on the page.\n")

    def assertContainsLink(doc: Document, text: String, href: String): Assertion =
      assert(
        doc.getElementsContainingText(text).attr("href").contains(href),
        s"\n\nLink $href was not rendered on the page\n"
      )
    def assertContainsLinkByContainingClass(doc: Document, href: String, passType: String): Assertion = {
      assert(
        doc.getElementsByClass("show-on-phones").select(s"a#$passType").attr("href").contains(href),
        s"\n\nLink $href was not rendered on the page\n"
      )
    }
  }

  "Main" when {

    "rendering the view" must {

      "render the correct nino" in new LocalSetup {
        assertContainsText(doc, generatedNino.nino)
      }

      "render the save to Google Wallet link on mobile" in new LocalSetup {
        wireMockServer.stubFor(get(s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-google-pass?passId=$fakeGooglePassId").willReturn(ok(fakeBase64String)))
        assertContainsLinkByContainingClass(doc, s"/get-google-pass?passId=$fakeGooglePassId", "google")
      }

      "render the save to Apple Wallet link on mobile" in new LocalSetup {
        wireMockServer.stubFor(get(s"${frontendAppConfig.findMyNinoServiceUrl}/find-my-nino-add-to-wallet/get-pass-card?passId=$fakeApplePassId").willReturn(ok(fakeBase64String)))
        assertContainsLinkByContainingClass(doc, s"/get-pass-card?passId=$fakeApplePassId", "apple")
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

        val href: String =  routes.AuthController
          .signout(Some(RedirectUrl(frontendAppConfig.getFeedbackSurveyUrl(frontendAppConfig.defaultOrigin))), None)
          .url

        assertContainsLink(doc, "Sign Out", URLDecoder.decode(href, "UTF-8"))
      }
    }
  }
}
