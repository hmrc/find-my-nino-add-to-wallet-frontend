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

package util

import controllers.auth.requests.UserRequest
import models.individualDetails.IndividualDetailsDataCache
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolments}
import uk.gov.hmrc.domain.Nino

object UserRequestFixture {

  def buildUserRequest[A](
    nino: Option[Nino] = Some(Fixtures.fakeNino),
    confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
    individualDetailsData: IndividualDetailsDataCache = Fixtures.fakeIndividualDetailsDataCache,
    enrolments: Enrolments,
    request: Request[A],
    trustedHelper: Option[TrustedHelper] = None
  ): UserRequest[A] =
    UserRequest(
      nino,
      confidenceLevel,
      individualDetailsData,
      enrolments,
      request,
      trustedHelper
    )
}
