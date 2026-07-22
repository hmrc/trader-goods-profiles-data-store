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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.FileReadyUnseen

import java.time.Instant

class DownloadDataSummarySpec extends AnyFreeSpec with Matchers {

  "DownloadDataSummary.format" - {

    "must round-trip when file information is present" in {
      val summary = DownloadDataSummary(
        summaryId = "summary-1",
        eori = "GB123456789000",
        status = FileReadyUnseen,
        createdAt = Instant.parse("2026-07-21T10:15:30Z"),
        expiresAt = Instant.parse("2026-08-20T10:15:30Z"),
        fileInfo = Some(FileInfo("records.csv", 1024, "30"))
      )

      Json.toJson(summary).as[DownloadDataSummary] mustBe summary
    }

    "must round-trip when file information is absent" in {
      val summary = DownloadDataSummary(
        summaryId = "summary-2",
        eori = "GB123456789000",
        status = FileReadyUnseen,
        createdAt = Instant.parse("2026-07-21T10:15:30Z"),
        expiresAt = Instant.parse("2026-08-20T10:15:30Z"),
        fileInfo = None
      )

      Json.toJson(summary).as[DownloadDataSummary] mustBe summary
    }
  }

  "DownloadDataSummary.mongoFormat" - {

    "must write and read mongo json" in {
      val summary = DownloadDataSummary(
        summaryId = "summary-3",
        eori = "GB123456789000",
        status = FileReadyUnseen,
        createdAt = Instant.parse("2026-07-21T10:15:30Z"),
        expiresAt = Instant.parse("2026-08-20T10:15:30Z"),
        fileInfo = Some(FileInfo("records.csv", 1024, "30"))
      )

      DownloadDataSummary.mongoFormat.reads(DownloadDataSummary.mongoFormat.writes(summary)).get mustBe summary
    }
  }
}
