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

package util

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar

import java.util.Base64

class GoogleCredentialsHelperSpec extends SpecBase with MockitoSugar {

  "GoogleCredentialsHelper" - {

    "should create GoogleCredentials from a base64-encoded key" ignore {
      val helper = new GoogleCredentialsHelper()
      // Replace with a valid base64-encoded key
      val base64Key = ""

      val credentials = helper.createGoogleCredentials(base64Key)

      // Assert that the credentials were created successfully
      assert(credentials != null)
    }

    "should create GoogleCredentials with the correct scope" ignore {
      val helper = new GoogleCredentialsHelper()
      // Replace with a valid base64-encoded key
      val base64Key = ""

      val credentials = helper.createGoogleCredentials(base64Key)

      // Assert that the credentials have the expected scope
      assert(credentials.contains("https://www.googleapis.com/auth/wallet_object.issuer"))
    }
  }

}
