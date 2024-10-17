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

package uk.gov.hmrc.tradergoodsprofilesdatastore.actions

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.RetireFilesActionImpl
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.FileReadySeen
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetireFilesActionSpec extends SpecBase with GetRecordsResponseUtil {

  class Harness(
    downloadDataSummaryRepository: DownloadDataSummaryRepository
  ) extends RetireFilesActionImpl(downloadDataSummaryRepository) {
    def callFilter[A](request: IdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  "RetireFilesAction" - {

    "must remove summaries if too old" in {

      val requestEori   = "GB123456789099"
      val fileName      = "fileName"
      val fileSize      = 600
      val fileCreated   = Instant.now.minus(40, ChronoUnit.DAYS)
      val createdAt     = Instant.now.minus(41, ChronoUnit.DAYS)
      val id            = java.util.UUID.randomUUID().toString
      val retentionDays = "30"

      val downloadDataSummary = DownloadDataSummary(
        id,
        requestEori,
        FileReadySeen,
        createdAt,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays))
      )

      val anotherDownloadDataSummary = DownloadDataSummary(
        java.util.UUID.randomUUID().toString,
        requestEori,
        FileReadySeen,
        createdAt,
        Some(FileInfo(fileName, fileSize, fileCreated, "1000"))
      )

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]

      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(
        Seq(downloadDataSummary, anotherDownloadDataSummary)
      )

      when(mockDownloadDataSummaryRepository.deleteMany(any(), any())) thenReturn Future.successful(
        1
      )

      val action = new Harness(mockDownloadDataSummaryRepository)

      val result = action
        .callFilter(IdentifierRequest(FakeRequest(), "testUserId", requestEori, AffinityGroup.Individual))
        .futureValue

      result mustEqual None

      verify(mockDownloadDataSummaryRepository).get(eqTo(requestEori))
      verify(mockDownloadDataSummaryRepository).deleteMany(eqTo(requestEori), any())
    }
  }
}
