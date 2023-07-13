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

import com.google.auth.oauth2.GoogleCredentials

import java.io.ByteArrayInputStream
import java.util.{Base64, Collections}


class GoogleCredentialsHelper {
  def createGoogleCredentials(key: String, refresh: Boolean): String = {
    val scope = "https://www.googleapis.com/auth/wallet_object.issuer"
    val keyAsStream = new ByteArrayInputStream(Base64.getDecoder.decode(key))
    val credentials: GoogleCredentials = GoogleCredentials.fromStream(keyAsStream).createScoped(Collections.singletonList(scope))
    if(refresh) credentials.refresh()
    GoogleCredentialsSerializer.serializeToBase64String(credentials)
  }
}
