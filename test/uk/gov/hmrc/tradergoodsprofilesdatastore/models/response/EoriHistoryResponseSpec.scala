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

import play.api.libs.json.Json
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase

class EoriHistoryResponseSpec extends SpecBase {
  "EoriHistoryResponse" - {
    "must serialize when all fields are populated" in {
      val eoriHistoryResponse = EoriHistoryResponse(
        eoriHistory = Seq(
          EoriHistoricItem(
            eori = "GB1234567890",
            validFrom = java.time.Instant.parse("2024-10-12T16:12:34Z"),
            validUntil = Some(java.time.Instant.parse("2024-10-12T16:12:34Z"))
          )
        )
      )

      val json = Json.toJson(eoriHistoryResponse)
      json.validate[EoriHistoryResponse].asOpt.value mustBe eoriHistoryResponse
    }

    "must serialize when validUntil is missing" in {
      val eoriHistoryResponse = EoriHistoryResponse(
        eoriHistory = Seq(
          EoriHistoricItem(
            eori = "GB1234567890",
            validFrom = java.time.Instant.parse("2024-10-12T16:12:34Z"),
            validUntil = None
          )
        )
      )

      val json = Json.toJson(eoriHistoryResponse)
      json.validate[EoriHistoryResponse].asOpt.value mustBe eoriHistoryResponse
    }

    "must deserialize into a sorted sequence" in {
      val json = Json.parse(
        """
          |{
          |  "eoriHistory": [
          |    {
          |      "eori": "GB1234567890",
          |      "validFrom": "2024-09-12T16:12:34Z"
          |    },
          |    {
          |      "eori": "GB1234567890",
          |      "validFrom": "2024-10-12T16:12:34Z",
          |      "validUntil": "2024-10-12T16:12:34Z"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val eoriHistoryResponse = json.as[EoriHistoryResponse]

      eoriHistoryResponse.eoriHistory mustBe Seq(
        EoriHistoricItem(
          eori = "GB1234567890",
          validFrom = java.time.Instant.parse("2024-10-12T16:12:34Z"),
          validUntil = Some(java.time.Instant.parse("2024-10-12T16:12:34Z"))
        ),
        EoriHistoricItem(
          eori = "GB1234567890",
          validFrom = java.time.Instant.parse("2024-09-12T16:12:34Z"),
          validUntil = None
        )
      )
    }
  }

}
