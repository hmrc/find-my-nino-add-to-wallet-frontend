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

package messages

import base.SpecBase
import play.api.i18n.Messages
import play.api.i18n.Messages.MessageSource
import scala.io.Source

class MessagesSpec extends SpecBase{
  private val MatchSingleQuoteOnly = """\w+'{1}\w+""".r
  private val MatchBacktickQuoteOnly = """`+""".r

  private val englishMessages = parseMessages("conf/messages.en")
  private val welshMessages = parseMessages("conf/messages.cy")

  "All message files" - {

    "must have a non-empty message for each key" in {
      assertNonEmpty("English", englishMessages)
      assertNonEmpty("Welsh", welshMessages)
    }

    "must have no unescaped single quotes in value" in {
      assertCorrectUseOfQuotes("English", englishMessages)
      assertCorrectUseOfQuotes("Welsh", welshMessages)
    }
    "must have a resolvable message for keys which take args" in {
      countMessagesWithArgs(welshMessages).size mustBe countMessagesWithArgs(
        englishMessages
      ).size
    }
  }

  private def parseMessages(filename: String): Map[String, String] =
    Messages.parse(
      new MessageSource {
        override def read: String = Source.fromFile(filename).mkString
      },
      filename
    ) match {
      case Right(messages) => messages
      case Left(e) => throw e
    }

  private def countMessagesWithArgs(messages: Map[String, String]) =
    messages.values.filter(_.contains("{0}"))

  private def assertNonEmpty(label: String, messages: Map[String, String]): Unit =
    messages.foreach { case (key: String, value: String) =>
      withClue(
        s"In $label, there is an empty value for the key:[$key][$value]"
      ) {
        value.trim.isEmpty mustBe false
      }
    }

  private def assertCorrectUseOfQuotes(label: String, messages: Map[String, String]): Unit =
    messages.foreach { case (key: String, value: String) =>
      withClue(
        s"In $label, there is an unescaped or invalid quote:[$key][$value]"
      ) {
        MatchSingleQuoteOnly.findFirstIn(value).isDefined mustBe false
        MatchBacktickQuoteOnly.findFirstIn(value).isDefined mustBe false
      }
    }
}