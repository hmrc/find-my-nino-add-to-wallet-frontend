package itutil.config

import java.util.UUID
import itutil.config.IntegrationTestConstants._
import models._
import play.api.libs.json._


import scala.language.postfixOps

object IntegrationTestConstants {
  val testApiVersion = 2
  val testJourneyId = "Jid123"
  val testCsrfToken = () => UUID.randomUUID().toString
  val useWelshCookieName = "Use-Welsh"
}


