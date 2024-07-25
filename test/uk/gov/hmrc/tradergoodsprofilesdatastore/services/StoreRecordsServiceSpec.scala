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
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logging
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.recursivePageSize
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

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

  val service = new StoreRecordsService(mockRouterConnector, mockRecordsRepository, mockRecordsSummaryRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRouterConnector, mockRecordsRepository, mockRecordsSummaryRepository)
  }

  "StoreRecordsService" - {
    "storeRecords" - {
      "must store all records in data-store with multiple pages" in {
        val totalRecordsNum = 2000
        val requestEori     = "GB123456789099"
        when(mockRecordsRepository.getCountWithInactive(any())) thenReturn Future.successful(totalRecordsNum)
        when(mockRecordsSummaryRepository.set(any(), any())) thenReturn Future.successful(true)
        when(mockRecordsRepository.updateRecords(any(), any())) thenReturn Future.successful(Done)
        when(mockRouterConnector.getRecords(any(), any(), any(), any())(any())) thenReturn (
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = getTestRecords(requestEori, recursivePageSize),
              Pagination(totalRecordsNum, 0, 4, Some(1), None)
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 1, 4, Some(2), Some(0))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 2, 4, Some(3), Some(1))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 3, 4, None, Some(2))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 1, totalRecordsNum, Some(1), None)
            )
          )
        )
        val result          = await(service.storeRecords(requestEori, None)(hc))

        result shouldBe false

        val done = Promise[Done]
        eventually {
          verify(mockRouterConnector, times(5)).getRecords(any(), any(), any(), any())(any())
          verify(mockRecordsRepository, times(4)).updateRecords(any(), any())
          verify(mockRecordsSummaryRepository, times(5)).set(any(), any())
          verify(mockRecordsRepository).getCountWithInactive(any())
          done.success(Done)
        }
        done.future.futureValue
      }

      "must store all records in data-store with one page" in {
        val totalRecordsNum = recursivePageSize
        val requestEori     = "GB123456789099"

        when(mockRecordsRepository.getCountWithInactive(any())) thenReturn Future.successful(totalRecordsNum)
        when(mockRecordsSummaryRepository.set(any(), any())) thenReturn Future.successful(true)
        when(mockRecordsRepository.updateRecords(any(), any())) thenReturn Future.successful(Done)
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
              goodsItemRecords = getTestRecords(requestEori, recursivePageSize),
              Pagination(totalRecordsNum, 0, 6, None, None)
            )
          )

        val result = await(service.storeRecords(requestEori, None)(hc))

        result.value shouldBe true

        verify(mockRouterConnector, times(2))
          .getRecords(any(), any(), any(), any())(any())
        verify(mockRecordsRepository, times(1)).updateRecords(any(), any())
        verify(mockRecordsRepository).getCountWithInactive(any())
      }

      "must store latest records in data-store with one page" in {
        val totalRecordsNum = recursivePageSize
        val requestEori     = "GB123456789099"

        when(mockRecordsRepository.getCountWithInactive(any())) thenReturn Future.successful(totalRecordsNum)
        when(mockRecordsSummaryRepository.set(any(), any())) thenReturn Future.successful(true)
        when(mockRecordsRepository.updateRecords(any(), any())) thenReturn Future.successful(Done)
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
              goodsItemRecords = getTestRecords(requestEori, recursivePageSize),
              Pagination(totalRecordsNum, 0, 6, None, None)
            )
          )

        val lastUpdatedDate = Some("2024-10-12T16:12:34Z")
        val result          = await(service.storeRecords(requestEori, lastUpdatedDate)(hc))

        result.value shouldBe true

        verify(mockRouterConnector)
          .getRecords(any(), eqTo(lastUpdatedDate), any(), any())(any())
        verify(mockRouterConnector)
          .getRecords(any(), eqTo(None), any(), any())(any())
        verify(mockRecordsRepository, times(1)).updateRecords(any(), any())
        verify(mockRecordsRepository).getCountWithInactive(any())
      }
    }
  }
}
