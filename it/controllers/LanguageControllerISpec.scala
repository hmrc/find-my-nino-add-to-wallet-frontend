package controllers

import itutil.IntegrationSpecBase
import play.api.http.Status._
import play.api.i18n.MessagesApi

class LanguageControllerISpec extends IntegrationSpecBase {

  implicit lazy val messagesAPI: MessagesApi = app.injector.instanceOf[MessagesApi]

  s"${controllers.routes.LanguageSwitchController.switchToLanguage("english")}" must {
    "swap to english and redirect back" in {
      val response = await(buildClientLanguage("english", "testRef").get)
      response.status shouldBe SEE_OTHER
      response.cookie(messagesApi.langCookieName).get.value shouldBe "en"
    }
  }

  s"${controllers.routes.LanguageSwitchController.switchToLanguage("cymraeg")}" must {
    "swap to welsh and redirect back" in {
      val response = await(buildClientLanguage("cymraeg", "testRef").get)
      response.status shouldBe SEE_OTHER
      response.cookie(messagesApi.langCookieName).get.value shouldBe "cy"
    }
  }

}