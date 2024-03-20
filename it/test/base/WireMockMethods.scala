/*
 * Copyright 2019 HM Revenue & Customs
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

package base

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{Json, Writes}

import java.net.URI

trait WireMockMethods {

  def when(method: HTTPMethod, uri: URI, headers: Map[String, String] = Map.empty): Mapping = {
    new Mapping(method, uri, headers, None)
  }

  class Mapping(method: HTTPMethod, uri: URI, headers: Map[String, String], body: Option[String]) {
    private val mapping = {

      val queryParams = Option(uri.getQuery).map(_.split("&").map(x => x.splitAt(x.indexOf("="))).toMap).getOrElse(Map())

      val uriMapping = method.wireMockMapping(urlPathMatching(uri.getPath))

      val uriMappingWithHeaders = headers.foldLeft(uriMapping) {
        case (m, (key, value)) => m.withHeader(key, equalTo(value))
      }

      val uriMappingWithOptionQueryParams = queryParams.foldLeft(uriMappingWithHeaders) {
        case (m, (key, value)) => m.withQueryParam(key, equalTo(value.tail))
      }

      body match {
        case Some(extractedBody) => uriMappingWithOptionQueryParams.withRequestBody(equalTo(extractedBody))
        case None => uriMappingWithOptionQueryParams
      }
    }

    def thenReturn[T](status: Int, body: Option[T])(implicit writes: Writes[T]): StubMapping = {
      val stringBody = body.map(Json.toJson(_).toString)
      thenReturnInternal(status, Map.empty, stringBody)
    }

    def thenReturn(status: Int, headers: Map[String, String] = Map.empty): StubMapping = {
      thenReturnInternal(status, headers, None)
    }

    private def thenReturnInternal(status: Int, headers: Map[String, String], body: Option[String]): StubMapping = {
      val response = {
        val statusResponse = aResponse().withStatus(status)
        val responseWithHeaders = headers.foldLeft(statusResponse) {
          case (res, (key, value)) => res.withHeader(key, value)
        }
        body match {
          case Some(extractedBody) => responseWithHeaders.withBody(extractedBody)
          case None => responseWithHeaders
        }
      }

      stubFor(mapping.willReturn(response))
    }
  }

  sealed trait HTTPMethod {
    def wireMockMapping(pattern: UrlPattern): MappingBuilder
  }

  case object POST extends HTTPMethod {
    override def wireMockMapping(pattern: UrlPattern): MappingBuilder = post(pattern)
  }

  case object PUT extends HTTPMethod {
    override def wireMockMapping(pattern: UrlPattern): MappingBuilder = put(pattern)
  }

  case object DELETE extends HTTPMethod {
    override def wireMockMapping(pattern: UrlPattern): MappingBuilder = delete(pattern)
  }

  case object GET extends HTTPMethod {
    override def wireMockMapping(pattern: UrlPattern): MappingBuilder = get(pattern)
  }
}
