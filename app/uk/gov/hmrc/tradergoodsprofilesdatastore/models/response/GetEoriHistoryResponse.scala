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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response

case class GetEoriHistoryResponse(
  eoriHistory: Seq[EoriHistoryItem]
)

object GetEoriHistoryResponse {
  implicit val recordsReads: Reads[GetEoriHistoryResponse] = (json: JsValue) =>
    JsSuccess(
      response.GetEoriHistoryResponse(
        (json \ "eoriHistory").as[Seq[EoriHistoryItem]]
      )
    )

  implicit val recordsWrites: Writes[GetEoriHistoryResponse] = (response: GetEoriHistoryResponse) =>
    Json.obj(
      "eoriHistory" -> response.eoriHistory
    )
}
