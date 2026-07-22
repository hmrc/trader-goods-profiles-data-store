/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class CreateRecordResponseSpec extends AnyFreeSpec with Matchers {

  "CreateRecordResponse" - {

    "must write to json" in {
      Json.toJson(CreateRecordResponse("record-123")) mustBe Json.obj(
        "recordId" -> "record-123"
      )
    }

    "must read from json" in {
      Json.obj("recordId" -> "record-123").as[CreateRecordResponse] mustBe CreateRecordResponse("record-123")
    }
  }
}
