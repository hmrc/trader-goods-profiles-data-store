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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models.response

import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.ResponseModelSupport.removeNulls

import java.time.Instant
case class GoodsItemRecords(
  eori: String,
  actorId: String,
  recordId: String,
  traderRef: String,
  comcode: String,
  accreditationStatus: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[Int],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: String,
  nirmsNumber: String,
  niphlNumber: String,
  locked: Boolean,
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object GoodsItemRecords {

  implicit val goodsItemRecordsWrites: Writes[GoodsItemRecords] = (goodsItemRecords: GoodsItemRecords) =>
    removeNulls(
      Json.obj(
        "eori"                     -> goodsItemRecords.eori,
        "actorId"                  -> goodsItemRecords.actorId,
        "recordId"                 -> goodsItemRecords.recordId,
        "traderRef"                -> goodsItemRecords.traderRef,
        "comcode"                  -> goodsItemRecords.comcode,
        "accreditationStatus"      -> goodsItemRecords.accreditationStatus,
        "goodsDescription"         -> goodsItemRecords.goodsDescription,
        "countryOfOrigin"          -> goodsItemRecords.countryOfOrigin,
        "category"                 -> goodsItemRecords.category,
        "assessments"              -> goodsItemRecords.assessments,
        "supplementaryUnit"        -> goodsItemRecords.supplementaryUnit,
        "measurementUnit"          -> goodsItemRecords.measurementUnit,
        "comcodeEffectiveFromDate" -> goodsItemRecords.comcodeEffectiveFromDate,
        "comcodeEffectiveToDate"   -> goodsItemRecords.comcodeEffectiveToDate,
        "version"                  -> goodsItemRecords.version,
        "active"                   -> goodsItemRecords.active,
        "toReview"                 -> goodsItemRecords.toReview,
        "reviewReason"             -> goodsItemRecords.reviewReason,
        "declarable"               -> goodsItemRecords.declarable,
        "ukimsNumber"              -> goodsItemRecords.ukimsNumber,
        "nirmsNumber"              -> goodsItemRecords.nirmsNumber,
        "niphlNumber"              -> goodsItemRecords.niphlNumber,
        "locked"                   -> goodsItemRecords.locked,
        "createdDateTime"          -> goodsItemRecords.createdDateTime,
        "updatedDateTime"          -> goodsItemRecords.updatedDateTime
      )
    )

  implicit val goodsItemRecordsReads: Reads[GoodsItemRecords] = (json: JsValue) =>
    JsSuccess(
      GoodsItemRecords(
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "recordId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "accreditationStatus").as[String],
        (json \ "goodsDescription").as[String],
        (json \ "countryOfOrigin").as[String],
        (json \ "category").as[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[Int],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[Instant],
        (json \ "comcodeEffectiveToDate").asOpt[Instant],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ "ukimsNumber").as[String],
        (json \ "nirmsNumber").as[String],
        (json \ "niphlNumber").as[String],
        (json \ "locked").as[Boolean],
        (json \ "createdDateTime").as[Instant],
        (json \ "updatedDateTime").as[Instant]
      )
    )

  implicit val goodsItemRecordsFormat: Format[GoodsItemRecords] =
    Format(goodsItemRecordsReads, goodsItemRecordsWrites)

}
