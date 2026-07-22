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

package uk.gov.hmrc.tradergoodsprofilesdatastore.utils

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsNull, Json}

class ResponseModelSupportSpec extends AnyFreeSpec with Matchers {

  "removeNulls" - {

    "must remove top-level and nested null fields" in {
      val json = Json.obj(
        "keep"   -> "value",
        "drop"   -> JsNull,
        "nested" -> Json.obj(
          "keepNested" -> "nested-value",
          "dropNested" -> JsNull
        )
      )

      ResponseModelSupport.removeNulls(json) mustBe Json.obj(
        "keep"   -> "value",
        "nested" -> Json.obj(
          "keepNested" -> "nested-value"
        )
      )
    }
  }
}
