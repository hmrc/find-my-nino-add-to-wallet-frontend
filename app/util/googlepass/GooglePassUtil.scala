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

import config.ConfigDecorator
import models.{GooglePassCard, GooglePassTextRow}

import javax.inject.Inject

class GooglePassUtil @Inject()(config: ConfigDecorator, createGenericPrivatePass: CreateGenericPrivatePass) {

  val issuerId: String = config.googleIssuerId
  val id: String = "hmrc.nino.pass.gov.uk"
  val key: String = config.googleKey

  def createGooglePass(name: String, nino: String): String = {

    val googlePassCardContent = createGooglePassCardContent(name, nino)

    val jwt = createGenericPrivatePass.createJwt(id, issuerId, key, googlePassCardContent)

    val saveUrl = "https://pay.google.com/gp/v/save/" + jwt
    saveUrl
  }

  private def createGooglePassCardContent(name: String, nino: String): GooglePassCard = {
    val pass: GooglePassCard = GooglePassCard(
      header = "HM Revenue & Customs",
      title = "National Insurance Number",
      rows = Some(Array(
        GooglePassTextRow(
          id = Some("row2left"),
          header = Some("Name"),
          body = Some(name)),
        GooglePassTextRow(
          id = Some("row3left"),
          header = Some("National Insurance Number"),
          body = Some(nino)),
        GooglePassTextRow(
          id = Some("row4left"),
          header = None,
          body = Some("This is not proof of your identity or your right to work in the UK. Only share where necessary."))
        )),
      hexBackgroundColour = "#008670",
      language = "en"
    )
    pass
  }

}
