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

import java.time.LocalDate

case class EoriHistoryResponse(
  eoriHistory: Seq[EoriHistoricItem]
)

object EoriHistoryResponse {
  implicit val reads: Reads[EoriHistoryResponse] = (
    (__ \ "eoriHistory").read[Seq[EoriHistoricItem]].map(_.sortBy(_.validFrom)(Ordering[LocalDate].reverse))
  ).map(EoriHistoryResponse.apply)

  implicit val writes: Writes[EoriHistoryResponse] = Json.writes[EoriHistoryResponse]
}
