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

package models.individualDetails

import org.apache.commons.lang3.StringUtils
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

case class IndividualDetailsData(
                              fullName: String,
                              firstForename: String,
                              surname: String,
                              initialsName: String,
                              nino: String,
                              address: Option[AddressData]
                              )

case class IndividualDetailsDataCache(
                                       id: String,
                                       individualDetailsData: Option[IndividualDetailsData],
                                       lastUpdated: Instant = LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC)
 )

object IndividualDetailsDataCache {
  private val individualDetailsDataFormat: OFormat[IndividualDetailsData] = {
    ( (__ \ "fullName").format[String]
      ~ (__ \ "firstForename").format[String]
      ~ (__ \ "surname").format[String]
      ~ (__ \ "initialsName").format[String]
      ~ (__ \ "nino").format[String]
      ~ (__ \ "address").formatNullable[AddressData]
      )(IndividualDetailsData.apply, unlift(IndividualDetailsData.unapply))
  }

  val individualDetailsDataCacheFormat: OFormat[IndividualDetailsDataCache] = {
    ((__ \ "id").format[String]
      ~ (__ \ "individualDetails").formatNullable[IndividualDetailsData](individualDetailsDataFormat)
      ~ (__ \ "lastUpdated").format[Instant](instantFormat)
      )(IndividualDetailsDataCache.apply, unlift(IndividualDetailsDataCache.unapply))
  }

  implicit class IndividualDetailsDataOps(private val individualDetailsData:IndividualDetailsDataCache) extends AnyVal {

    def getFullName: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.fullName
      case _        => StringUtils.EMPTY
    }

    def getNino: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.nino
      case _        => StringUtils.EMPTY
    }

    def getFirstForename: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.firstForename
      case _        => StringUtils.EMPTY
    }

    def getLastName: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.surname
      case _        => StringUtils.EMPTY
    }

    def getInitialsName: String = individualDetailsData.individualDetailsData match {
      case Some(id) => id.initialsName
      case _        => StringUtils.EMPTY
    }

    def getAddress: Option[AddressData] = individualDetailsData.individualDetailsData match {
      case Some(id) => id.address
      case _        => None
    }

    def getAddressLines: List[String] = individualDetailsData.individualDetailsData match {
      case Some(id) => id.address match {
        case Some(address) =>
          List(
            address.addressLine1.value,
            address.addressLine2.value,
            address.addressLine3.map(_.value).getOrElse(""),
            address.addressLine4.map(_.value).getOrElse(""),
            address.addressLine5.map(_.value).getOrElse("")
          ).filter(_.nonEmpty)
        case _ => List.empty
      }
      case _ => List.empty
    }

    def getPostCode: Option[String] = individualDetailsData.individualDetailsData match {
      case Some(id) => id.address.flatMap(addr => addr.addressPostcode.map(postcode => postcode.value))
      case _ => Some(StringUtils.EMPTY)
    }

  }

}
