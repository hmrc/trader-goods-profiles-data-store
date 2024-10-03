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
case class EoriHistoricItem(
  eori: String,
  validFrom: Instant,
  validTo: Instant
)

object EoriHistoricItem {
  implicit val reads: Reads[EoriHistoricItem] = (json: JsValue) =>
    JsSuccess(
      EoriHistoricItem(
        (json \ "eori").as[String],
        (json \ "validFrom").as[Instant],
        (json \ "validTo").as[Instant]
      )
    )

  implicit val writes: Writes[EoriHistoricItem] = (historyItem: EoriHistoricItem) =>
    Json.obj(
      "eori"      -> historyItem.eori,
      "validFrom" -> historyItem.validFrom,
      "validTo"   -> historyItem.validTo
    )

}
