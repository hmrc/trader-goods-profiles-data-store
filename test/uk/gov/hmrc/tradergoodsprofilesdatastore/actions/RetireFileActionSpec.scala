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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.ACCEPTED
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.RetireFileActionImpl
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileReadySeen, FileReadyUnseen, RequestFile}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.StoreRecordsService
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetireFileActionSpec extends SpecBase with GetRecordsResponseUtil {

  class Harness(
    downloadDataSummaryRepository: DownloadDataSummaryRepository
  ) extends RetireFileActionImpl(downloadDataSummaryRepository) {
    def callFilter[A](request: IdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  "RetireFileAction" - {
    "must add summary if not present" in {

      val requestEori = "GB123456789099"

      val newDownloadDataSummary = DownloadDataSummary(
        requestEori,
        RequestFile,
        None
      )

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(
        None
      )
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(
        Done
      )

      val action = new Harness(mockDownloadDataSummaryRepository)

      val result = action
        .callFilter(IdentifierRequest(FakeRequest(), "testUserId", requestEori, AffinityGroup.Individual))
        .futureValue

      result mustEqual None

      verify(mockDownloadDataSummaryRepository).get(eqTo(requestEori))
      verify(mockDownloadDataSummaryRepository).set(eqTo(newDownloadDataSummary))

    }
    "must update summary if too old" in {

      val requestEori   = "GB123456789099"
      val fileName      = "fileName"
      val fileSize      = 600
      val fileCreated   = Instant.now.minus(40, ChronoUnit.DAYS)
      val retentionDays = "30"
      val fileType      = "CSV"

      val downloadDataSummary = DownloadDataSummary(
        requestEori,
        FileReadySeen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays, fileType))
      )

      val newDownloadDataSummary = DownloadDataSummary(
        requestEori,
        RequestFile,
        None
      )

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(
        Some(downloadDataSummary)
      )
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(
        Done
      )

      val action = new Harness(mockDownloadDataSummaryRepository)

      val result = action
        .callFilter(IdentifierRequest(FakeRequest(), "testUserId", requestEori, AffinityGroup.Individual))
        .futureValue

      result mustEqual None

      verify(mockDownloadDataSummaryRepository).get(eqTo(requestEori))
      verify(mockDownloadDataSummaryRepository).set(eqTo(newDownloadDataSummary))

    }
    "must not update summary if not too old" in {

      val requestEori   = "GB123456789099"
      val fileName      = "fileName"
      val fileSize      = 600
      val fileCreated   = Instant.now.minus(20, ChronoUnit.DAYS)
      val retentionDays = "30"
      val fileType      = "CSV"

      val downloadDataSummary = DownloadDataSummary(
        requestEori,
        FileReadySeen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays, fileType))
      )

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]

      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(
        Some(downloadDataSummary)
      )
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(
        Done
      )

      val action = new Harness(mockDownloadDataSummaryRepository)

      val result = action
        .callFilter(IdentifierRequest(FakeRequest(), "testUserId", requestEori, AffinityGroup.Individual))
        .futureValue

      result mustEqual None

      verify(mockDownloadDataSummaryRepository).get(eqTo(requestEori))
      verify(mockDownloadDataSummaryRepository, never()).set(any())

    }
  }
}
