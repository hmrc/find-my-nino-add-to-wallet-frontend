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

package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import models.EnterYourNino

class EnterYourNinoFormProvider @Inject() extends Mappings {

  private val MAX_FULL_NAME_LENGTH = 100
  private val MAX_NINO_LENGTH = 20

  def apply(): Form[EnterYourNino] = Form(
     mapping(
      "fullName" -> text("enterYourNino.error.fullName.required")
        .verifying(maxLength(MAX_FULL_NAME_LENGTH, "enterYourNino.error.fullName.length")),
      "nino" -> text("enterYourNino.error.nino.required")
        .verifying(maxLength(MAX_NINO_LENGTH, "enterYourNino.error.nino.length"))
    )(EnterYourNino.apply)(EnterYourNino.unapply)
   )
 }
