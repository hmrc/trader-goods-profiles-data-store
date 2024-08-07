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

import java.time.Instant
case class GoodsItemRecord(
  recordId: String,
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  adviceStatus: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[BigDecimal],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: Option[String],
  nirmsNumber: Option[String],
  niphlNumber: Option[String],
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object GoodsItemRecord {
  private val mongoReads: Reads[GoodsItemRecord] = (json: JsValue) =>
    JsSuccess(
      GoodsItemRecord(
        (json \ "_id").as[String],
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "adviceStatus")
          .as[String],
        (json \ "goodsDescription").as[String],
        (json \ "countryOfOrigin").as[String],
        (json \ "category").as[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[BigDecimal],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[Instant],
        (json \ "comcodeEffectiveToDate").asOpt[Instant],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ "ukimsNumber").asOpt[String],
        (json \ "nirmsNumber").asOpt[String],
        (json \ "niphlNumber").asOpt[String],
        (json \ "createdDateTime").as[Instant],
        (json \ "updatedDateTime").as[Instant]
      )
    )

  private val mongoWrites: Writes[GoodsItemRecord] = (record: GoodsItemRecord) =>
    Json.obj(
      "_id"                      -> record.recordId,
      "eori"                     -> record.eori,
      "actorId"                  -> record.actorId,
      "traderRef"                -> record.traderRef,
      "comcode"                  -> record.comcode,
      "adviceStatus"             -> record.adviceStatus,
      "goodsDescription"         -> record.goodsDescription,
      "countryOfOrigin"          -> record.countryOfOrigin,
      "category"                 -> record.category,
      "assessments"              -> record.assessments,
      "supplementaryUnit"        -> record.supplementaryUnit,
      "measurementUnit"          -> record.measurementUnit,
      "comcodeEffectiveFromDate" -> record.comcodeEffectiveFromDate,
      "comcodeEffectiveToDate"   -> record.comcodeEffectiveToDate,
      "version"                  -> record.version,
      "active"                   -> record.active,
      "toReview"                 -> record.toReview,
      "reviewReason"             -> record.reviewReason,
      "declarable"               -> record.declarable,
      "ukimsNumber"              -> record.ukimsNumber,
      "nirmsNumber"              -> record.nirmsNumber,
      "niphlNumber"              -> record.niphlNumber,
      "createdDateTime"          -> record.createdDateTime,
      "updatedDateTime"          -> record.updatedDateTime
    )

  val goodsItemRecordsMongoFormat: Format[GoodsItemRecord] = Format(mongoReads, mongoWrites)

  implicit val reads: Reads[GoodsItemRecord] = (json: JsValue) =>
    JsSuccess(
      GoodsItemRecord(
        (json \ "recordId").as[String],
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "adviceStatus")
          .as[String],
        (json \ "goodsDescription").as[String],
        (json \ "countryOfOrigin").as[String],
        (json \ "category").as[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[BigDecimal],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[Instant],
        (json \ "comcodeEffectiveToDate").asOpt[Instant],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ "ukimsNumber").asOpt[String],
        (json \ "nirmsNumber").asOpt[String],
        (json \ "niphlNumber").asOpt[String],
        (json \ "createdDateTime").as[Instant],
        (json \ "updatedDateTime").as[Instant]
      )
    )

  implicit val writes: Writes[GoodsItemRecord] = (record: GoodsItemRecord) =>
    Json.obj(
      "recordId"                 -> record.recordId,
      "eori"                     -> record.eori,
      "actorId"                  -> record.actorId,
      "traderRef"                -> record.traderRef,
      "comcode"                  -> record.comcode,
      "adviceStatus"             -> record.adviceStatus,
      "goodsDescription"         -> record.goodsDescription,
      "countryOfOrigin"          -> record.countryOfOrigin,
      "category"                 -> record.category,
      "assessments"              -> record.assessments,
      "supplementaryUnit"        -> record.supplementaryUnit,
      "measurementUnit"          -> record.measurementUnit,
      "comcodeEffectiveFromDate" -> record.comcodeEffectiveFromDate,
      "comcodeEffectiveToDate"   -> record.comcodeEffectiveToDate,
      "version"                  -> record.version,
      "active"                   -> record.active,
      "toReview"                 -> record.toReview,
      "reviewReason"             -> record.reviewReason,
      "declarable"               -> record.declarable,
      "ukimsNumber"              -> record.ukimsNumber,
      "nirmsNumber"              -> record.nirmsNumber,
      "niphlNumber"              -> record.niphlNumber,
      "createdDateTime"          -> record.createdDateTime,
      "updatedDateTime"          -> record.updatedDateTime
    )

}
