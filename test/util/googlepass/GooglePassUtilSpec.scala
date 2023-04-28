/*
 * Copyright 2023 HM Revenue & Customs
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

package util.googlepass

import base.SpecBase
import config.ConfigDecorator
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import repositories.SessionRepository

import scala.concurrent.Future

class GooglePassUtilSpec extends SpecBase with MockitoSugar {


  val mockConfig: ConfigDecorator = mock[ConfigDecorator]
  val mockCreateGenericPrivatePass: CreateGenericPrivatePass = mock[CreateGenericPrivatePass]
  val mockSessionRepository = mock[SessionRepository]

  val googlePassUtil: GooglePassUtil = new GooglePassUtil(mockConfig, mockCreateGenericPrivatePass)

  when(mockSessionRepository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))
  when(mockCreateGenericPrivatePass.createJwt(any, any, any, any)) thenReturn "testJwt"


  "GooglePassUtil createGooglePass" - {
    "must return valid url" in {
      val result = googlePassUtil.createGooglePass("test name", "AB 01 23 45 C")
      result mustBe "https://pay.google.com/gp/v/save/testJwt"
    }
  }
}
