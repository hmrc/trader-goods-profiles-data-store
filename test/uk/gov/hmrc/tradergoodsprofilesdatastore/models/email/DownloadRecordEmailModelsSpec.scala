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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models.email

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class DownloadRecordEmailModelsSpec extends AnyFreeSpec with Matchers {

  "DownloadRecordEmailParameters" - {

    "must round-trip to json" in {
      val parameters = DownloadRecordEmailParameters("05 Sept 2026")

      Json.toJson(parameters).as[DownloadRecordEmailParameters] mustBe parameters
    }
  }

  "DownloadRecordEmailRequest" - {

    "must round-trip to json with the default template id" in {
      val request = DownloadRecordEmailRequest(
        to = Seq("one@example.com", "two@example.com"),
        parameters = DownloadRecordEmailParameters("05 Sept 2026")
      )

      val json = Json.toJson(request)

      (json \ "templateId").as[String] mustBe DownloadRecordEmailRequest.EmailTemplateId
      json.as[DownloadRecordEmailRequest] mustBe request
    }
  }

  "DownloadRecordFailureEmailParameters" - {

    "must round-trip to json" in {
      val parameters = DownloadRecordFailureEmailParameters()

      Json.toJson(parameters).as[DownloadRecordFailureEmailParameters] mustBe parameters
    }
  }

  "DownloadRecordFailureEmailRequest" - {

    "must round-trip to json with the default template id" in {
      val request = DownloadRecordFailureEmailRequest(
        to = Seq("one@example.com"),
        parameters = DownloadRecordFailureEmailParameters()
      )

      val json = Json.toJson(request)

      (json \ "templateId").as[String] mustBe DownloadRecordFailureEmailRequest.EmailTemplateId
      json.as[DownloadRecordFailureEmailRequest] mustBe request
    }
  }
}
