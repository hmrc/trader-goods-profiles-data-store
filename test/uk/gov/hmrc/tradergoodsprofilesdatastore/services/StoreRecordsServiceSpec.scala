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

package uk.gov.hmrc.tradergoodsprofilesdatastore.services

import org.apache.pekko.Done
import org.apache.pekko.util.Helpers.Requiring
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logging
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.pageSize
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import java.time.{Clock, Instant, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future, Promise}

class StoreRecordsServiceSpec
    extends SpecBase
    with MockitoSugar
    with GetRecordsResponseUtil
    with BeforeAndAfterEach
    with Logging {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val mockRouterConnector          = mock[RouterConnector]
  private val mockRecordsRepository        = mock[RecordsRepository]
  private val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]
  private val now                          = Instant.now().plus(1, ChronoUnit.DAYS)
  private val clock                        = Clock.fixed(now, ZoneOffset.UTC)

  val service = new StoreRecordsService(mockRouterConnector, mockRecordsRepository, mockRecordsSummaryRepository, clock)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRouterConnector, mockRecordsRepository, mockRecordsSummaryRepository)
  }

  "StoreRecordsService" - {

    "storeRecords" - {

      "must not update records summary when there are no new records" in {
        val totalRecordsNum = 0
        val requestEori     = "GB123456789099"

        when(mockRecordsSummaryRepository.set(any(), any(), any())) thenReturn Future.successful(Done)
        when(mockRecordsRepository.updateRecords(any(), any())) thenReturn Future.successful(Done)
        when(mockRecordsSummaryRepository.set(any(), any(), any())) thenReturn Future.successful(Done)

        when(
          mockRouterConnector.getRecords(
            any(),
            any(),
            any(),
            any()
          )(any())
        ) thenReturn
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq.empty,
              Pagination(totalRecordsNum, 0, 0, None, None)
            )
          )

        val result = await(service.storeRecords(requestEori, None)(hc))
        result.value shouldBe true

        verify(mockRouterConnector, times(1))
          .getRecords(eqTo(requestEori), eqTo(None), eqTo(Some(0)), eqTo(Some(pageSize)))(any())
        verify(mockRecordsRepository, times(0)).updateRecords(any(), any())
        verify(mockRecordsSummaryRepository, times(0)).set(any(), any(), any())
        verify(mockRecordsSummaryRepository, times(0)).update(any(), any(), any())
      }

      "must store records when there is a single page of records" in {
        val totalRecordsNum = pageSize
        val requestEori     = "GB123456789099"

        when(mockRecordsSummaryRepository.set(any(), any(), any())) thenReturn Future.successful(Done)
        when(mockRecordsRepository.updateRecords(any(), any())) thenReturn Future.successful(Done)
        when(mockRecordsSummaryRepository.update(any(), any(), any())) thenReturn Future.successful(Done)

        val firstRecord  = getGoodsItemRecord(requestEori)
          .copy(updatedDateTime = Instant.now().minus(2, ChronoUnit.DAYS))
        val secondRecord = getGoodsItemRecord(requestEori)
          .copy(updatedDateTime = Instant.now().minus(1, ChronoUnit.DAYS))

        when(
          mockRouterConnector.getRecords(
            any(),
            any(),
            any(),
            any()
          )(any())
        ) thenReturn
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(firstRecord, secondRecord),
              Pagination(totalRecordsNum, 0, 1, None, None)
            )
          )

        val result = await(service.storeRecords(requestEori, None)(hc))
        result shouldBe true

        verify(mockRouterConnector, times(1))
          .getRecords(any(), any(), any(), any())(any())
        verify(mockRecordsRepository, times(1)).updateRecords(any(), any())
        verify(mockRecordsSummaryRepository, times(1)).set(requestEori, None, now)
      }

      "must store records when there are multiple pages of records" in {
        val totalRecordsNum    = pageSize + 1
        val requestEori        = "GB123456789099"
        val oldDate            = Instant.now()
        val latestRecordUpdate = oldDate.plus(1, ChronoUnit.DAYS)

        when(mockRecordsSummaryRepository.set(any(), any(), any())) thenReturn Future.successful(Done)
        when(mockRecordsRepository.updateRecords(any(), any())) thenReturn Future.successful(Done)
        when(mockRouterConnector.getRecords(any(), any(), any(), any())(any())) thenReturn (
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = getTestRecords(requestEori, pageSize).map(_.copy(updatedDateTime = oldDate)),
              Pagination(totalRecordsNum, 0, 2, Some(1), None)
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori).copy(updatedDateTime = latestRecordUpdate)),
              Pagination(totalRecordsNum, 1, 2, None, Some(0))
            )
          )
        )

        val result = await(service.storeRecords(requestEori, None)(hc))
        result shouldBe false

        val done = Promise[Done]
        eventually {
          verify(mockRouterConnector, times(2)).getRecords(any(), any(), any(), any())(any())
          verify(mockRecordsRepository, times(2)).updateRecords(any(), any())
          verify(mockRecordsSummaryRepository, times(3)).set(any(), any(), any())
          verify(mockRecordsSummaryRepository, times(1))
            .set(requestEori, Some(Update(pageSize, totalRecordsNum)), oldDate)
          verify(mockRecordsSummaryRepository, times(1))
            .set(requestEori, Some(Update(pageSize + 1, totalRecordsNum)), latestRecordUpdate)
          verify(mockRecordsSummaryRepository, times(1)).set(requestEori, None, now)
          done.success(Done)
        }

        done.future.futureValue
      }

      "must remove the updating status when the recursive call fails" in {
        val totalRecordsNum = pageSize + 1
        val requestEori     = "GB123456789099"
        val oldDate         = Instant.now()

        when(mockRecordsSummaryRepository.set(any(), any(), any())) thenReturn Future.successful(Done)
        when(mockRecordsRepository.updateRecords(any(), any())) thenReturn Future.successful(Done)
        when(mockRouterConnector.getRecords(any(), any(), any(), any())(any())) thenReturn (
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = getTestRecords(requestEori, pageSize).map(_.copy(updatedDateTime = oldDate)),
              Pagination(totalRecordsNum, 0, 2, Some(1), None)
            )
          ),
          Future.failed(new RuntimeException())
        )

        val result = await(service.storeRecords(requestEori, None)(hc))
        result shouldBe false

        val done = Promise[Done]
        eventually {
          verify(mockRouterConnector, times(2)).getRecords(any(), any(), any(), any())(any())
          verify(mockRecordsRepository, times(1)).updateRecords(any(), any())
          verify(mockRecordsSummaryRepository, times(1)).set(any(), any(), any())
          verify(mockRecordsSummaryRepository, times(1)).set(requestEori, Some(Update(pageSize, pageSize + 1)), oldDate)
          verify(mockRecordsSummaryRepository, times(1)).update(requestEori, None, None)
          done.success(Done)
        }

        done.future.futureValue
      }
    }
  }
}
