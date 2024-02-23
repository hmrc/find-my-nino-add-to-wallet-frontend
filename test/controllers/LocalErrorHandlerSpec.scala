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

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc._
import play.api.test._
import play.twirl.api.Html
import views.html.ErrorTemplate
import views.html.templates.InternalServerErrorView

import scala.concurrent.ExecutionContext.Implicits.global

class LocalErrorHandlerSpec extends SpecBase with MockitoSugar {
  implicit val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
  implicit val messages: Messages = mock[Messages]


  "LocalErrorHandler" -  {

    "return the correct error template when standardErrorTemplate is called" in {
      val messagesApi = mock[MessagesApi]
      val fakeRequest = FakeRequest()
      val errorTemplate = mock[ErrorTemplate]
      val internalServerErrorView = mock[InternalServerErrorView]

      when(messagesApi.preferred(any[RequestHeader])).thenReturn(messages)

      when(errorTemplate.apply("title", "heading", "message")(fakeRequest, messages)).thenReturn(Html("Some HTML"))

      val localErrorHandler = new LocalErrorHandler(messagesApi, errorTemplate, internalServerErrorView)
      val result: Html = localErrorHandler.standardErrorTemplate("title", "heading", "message")(fakeRequest)

      result.body mustBe "Some HTML"
    }

    "return the correct error view when internalServerErrorTemplate is called" in {
      val messagesApi = mock[MessagesApi]
      val fakeRequest = FakeRequest()
      val errorTemplate = mock[ErrorTemplate]
      val internalServerErrorView = mock[InternalServerErrorView]

      when(messagesApi.preferred(any[RequestHeader])).thenReturn(messages)

      when(internalServerErrorView.apply()(fakeRequest, appConfig, messages)).thenReturn(Html("Some HTML"))

      val localErrorHandler = new LocalErrorHandler(messagesApi, errorTemplate, internalServerErrorView)
      val result: Html = localErrorHandler.internalServerErrorTemplate(fakeRequest)

      result.body mustBe "Some HTML"
    }
  }
}