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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Assessment

import java.time.Instant

case class PatchRecordRequest(
  actorId: String,
  traderRef: Option[String] = None,
  comcode: Option[String] = None,
  goodsDescription: Option[String] = None,
  countryOfOrigin: Option[String] = None,
  category: Option[Int] = None,
  assessments: Option[Seq[Assessment]] = None,
  supplementaryUnit: Option[BigDecimal] = None,
  measurementUnit: Option[String] = None,
  comcodeEffectiveFromDate: Option[Instant] = None,
  comcodeEffectiveToDate: Option[Instant] = None
)

object PatchRecordRequest {
  implicit val format: OFormat[PatchRecordRequest] = Json.format[PatchRecordRequest]
}
