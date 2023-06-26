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
import googleModels.{GooglePassCard, GooglePassTextRow}
import org.scalatestplus.mockito.MockitoSugar

class CreateGenericPrivatePassSpec extends SpecBase with MockitoSugar{

  val createGenericPrivatePass: CreateGenericPrivatePass = new CreateGenericPrivatePass

  val passCardContent: GooglePassCard = GooglePassCard(
    header = "HM Revenue & Customs",
    title = "National Insurance Number",
    rows = Some(Array(
      GooglePassTextRow(
        id = Some("row2left"),
        header = Some("Name"),
        body = Some("Test name")),
      GooglePassTextRow(
        id = Some("row3left"),
        header = Some("National Insurance Number"),
        body = Some("AB 12 34 56 C")),
      GooglePassTextRow(
        id = Some("row4left"),
        header = None,
        body = Some("This is not proof of your identity or your right to work in the UK. Only share where necessary."))
    )),
    hexBackgroundColour = "#008670",
    language = "en"
  )

  val templateGoogleServiceAccountKey = "ewogICJ0eXBlIjogInNlcnZpY2VfYWNjb3VudCIsCiAgInByb2plY3RfaWQiOiAic2NhLW5pbm8iLAogICJwcml2YXRlX2tleV9pZCI6ICI3N2NiMWNmYWExZmU4OGQwZTI3ODMzNmM2YzlhYWY0YzcyNGJmMTU4IiwKICAicHJpdmF0ZV9rZXkiOiAiLS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tXG5NSUlFdmdJQkFEQU5CZ2txaGtpRzl3MEJBUUVGQUFTQ0JLZ3dnZ1NrQWdFQUFvSUJBUURLYytucmZ5VjZpbVpEXG5sN0xIb0dKd2U1cGF2cDNEVlpaYkROMzZlTm1YcDFpR01XcjFKM3c5R2F4b2hwNUdGYTZJRmdGM3NWTG5jcDYvXG5zMzRwTUlUaXhLYVBndDZPa05sVXJ2TndzMkU0WnlsOGEvMmI2aS9aUDZHbTJLUnBOeFl6VWMrQzFDenVXM29EXG5JQzJ1aHI2dkZnZ21zNldoa24rTDV2cHRScEdZb1NhdHhUb3JmME1OQUYvME9OWjBUcUZ0Sml2ODMrdVkxYWdtXG4zYkVJVGJQdlYzVjVpd1BwOXZPSk5Tbng4NDRJeTYxNElNbjhOMWZQTy9Tc2Y3QVI2WGV0ZktOYVNGMHYyL2hWXG5IWnZxVG5mNnBoa0IvaTBuTk5hYmhVaWwwWXFxcmpGOGJlUUFuZ0R4RXBJaG1RZmdTZUxONjF6cjlIak92WHI0XG5iYVRJeTlPN0FnTUJBQUVDZ2dFQUFUZllxaVJyWUUrMjM4OCtNYzlOa3Y4R0VHZTNOcUZXMzBOZzQrSHRtcE9xXG5NbHlDYlQ2SWs5YmZYN0wzcVgrZ2tPRzVBOThGbXdPVE5aOXAzSlpBZThZbnptaVdMWkRDT0swNHo4UEo0UGRWXG44T2EyeHUrNHkvdURDSDJqTjUyRTU2MTZySDh1LzF3bzRYTkVjeGxpZFFFOVEvdXV2UjNPNEtqMHpDQXhrMmt2XG5hZVBRdW8xRkRVclNSZ1llbDR5cWlSRk03WmE2UktvSDVYVDExenhwYmVWZ0NYVHR2Wm9kNHBDWTNiWmZWU2xjXG5zY2JsUWFZVmVBaEJINzRpcHprWWoyUnZNZkpCZ0o4emRJNlFlOUFQbXFPd1FCKy9KTys2L3lyL2RmUkRlV0JwXG5ZUkptZEZlK2RROVZwWm1aQk45M2lZQ0NPVTJna2N0d3FrTWd4NFFGQ1FLQmdRRG9kLy9NMjlwUkhRR1ZaeEwrXG5yWUlNcjc0YllDSTQrbTVlaWpqK1NpaXB6OWpFcUx3RUgxYmxjek44T2ZWTTBZVkUxbW5mOHJHMEVlSUpyNlc1XG42MmFFYVJacjhOUWJlR2pzNzc5aHQvZEQ4S3h3RDBPREgxN1pjY25RNGxDOWZpQ2t2V0VCMklJU2dtTE9xeTNkXG5JbEo1Y2NxNXAwOXlrYVhxQm8wNythZ0V0d0tCZ1FEZThoc205WitUY0dieDBleUh0UnB0OWY0L3BJbmExTDNxXG5nek9MY25VYnF2dVlzQ2VZT1lPYllvUGpMMHBsMzUxK25UaE1sQU5LZ2dWcnZ6bkpINGlBUWowbTcxUW5uSmVRXG4ydHdsTFhGVlY3QmZSeUJwOTBFbVdFTDFUbVRUQ0xSeG1WTGZFOXJzMUJpdzVRNWMyNE9XbTRDdU5jOU9CbzJqXG5VTXNzNGFvTkhRS0JnRS9ockV6QTMyd3dBM05MVUFPanE2U2dYenNZOWFtT3BJNW9BTjJncmdoc0c1aTRRcU5JXG5BWndtMGZKR0dEeWxZcDRjNzVTODJNTi93YVNDYnZoTUk1WXNaa0FCS1dHbkpxY0VXMGRBNS82NG5RaUV0alpBXG5lVjlPOG9LTXdpSGJUV3hPaHg4VFB0OE5YdGFWaTlVSkRqNGRGQVJuc2EwMHowWnpxZVNLRFdwUkFvR0JBS09MXG5BRVg5Q2xreXJDR1o5Nk81UXpFRXNjUm40OFJHS1NhOXBmTVdQRGZXbm9kc09TOWVpVjlUemlHNmtCM1pBb1JkXG51bTNhYWV1ZkkzRGRydnNOaWFsa2JjMkE1TC9HREJ6em4yZ2FSTGZ4QW0xeUZLSUhBYUNxUUR3SWpNZU43Q2drXG42L2krYk9zcnp3ejhpaU90MTRLRWdjbkpxVSs0V1BCRGxUZFFOdGsxQW9HQkFPRHFGR1NjdEcxWVM1empQNnJVXG5EbHRQZnlnUFowNmZyN1VjYmoyV1RTejJQalVQWkoweFpJaXRqWVFQR29TdFVFbTRTR3RhNE1EWmlBTDZLK0tpXG5pK3FsdStuTG80dVFiMFhNUnMyS1dpZEcrYVlLSXNpazlhU3ZINHBlNC92NUVCRFZIQWc5QjE3SjhzeEN4V0M2XG5lQSsrRTh0UGxJMWxDamNzY1ZUUmREVHVcbi0tLS0tRU5EIFBSSVZBVEUgS0VZLS0tLS1cbiIsCiAgImNsaWVudF9lbWFpbCI6ICJwcml2YXRlLXBhc3Nlcy13ZWItY2xpZW50QHNjYS1uaW5vLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwKICAiY2xpZW50X2lkIjogIjExNzc5OTAwNDc0NzA0MTU1MzAzMiIsCiAgImF1dGhfdXJpIjogImh0dHBzOi8vYWNjb3VudHMuZ29vZ2xlLmNvbS9vL29hdXRoMi9hdXRoIiwKICAidG9rZW5fdXJpIjogImh0dHBzOi8vb2F1dGgyLmdvb2dsZWFwaXMuY29tL3Rva2VuIiwKICAiYXV0aF9wcm92aWRlcl94NTA5X2NlcnRfdXJsIjogImh0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL29hdXRoMi92MS9jZXJ0cyIsCiAgImNsaWVudF94NTA5X2NlcnRfdXJsIjogImh0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL3JvYm90L3YxL21ldGFkYXRhL3g1MDkvcHJpdmF0ZS1wYXNzZXMtd2ViLWNsaWVudCU0MHNjYS1uaW5vLmlhbS5nc2VydmljZWFjY291bnQuY29tIgp9Cg=="


  "CreateGenericPrivatePass createJwt" - {
    "must return some jwt string" in {
      val result = createGenericPrivatePass.createJwt("testID", "12345", templateGoogleServiceAccountKey, passCardContent)
      result.length must be > 0
    }
  }
}
