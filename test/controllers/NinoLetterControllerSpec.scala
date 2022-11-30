/*
 * Copyright 2022 HM Revenue & Customs
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
import models.PersonDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.print.PrintNationalInsuranceNumberView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class NinoLetterControllerSpec extends SpecBase {

  val pd = PersonDetails.personDetails

  "NinoLetter Controller" - {
    "must return OK and the correct view for a GET" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
          )
          .build()

      running(application) {

        val request = FakeRequest(GET, routes.NinoLetterController.onPageLoad().url)

        val view = application.injector.instanceOf[PrintNationalInsuranceNumberView]

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result).trim mustEqual
          view(pd,LocalDate.now.format(DateTimeFormatter.ofPattern("MM/YY")),saveNiLetterAsPdfLinkEnabled = true,PersonDetails.personDetails.person.nino.get)(request,messages(application)).toString().trim

        val requestPdf = FakeRequest(GET, routes.NinoLetterController.saveNationalInsuranceNumberAsPdf().url)
        val res2 = route(application, requestPdf).value
        contentAsString(res2).contains("national-insurance-letter.pdf").equals(true)

      }
    }

  }

}
