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
import org.apache.pekko.pattern.StatusReply.success
import org.apache.pekko.util.Helpers.Requiring
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Logger, Logging}
import play.api.i18n.Lang.logger
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.recursivePageSize
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class StoreRecordsServiceSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil with Logging {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  "StoreRecordsService" - {
    "storeRecords" - {
      "must store all records in data-store with multiple pages" in {
        val mockRouterConnector          = mock[RouterConnector]
        val mockRecordsRepository        = mock[RecordsRepository]
        val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]

        val service         = new StoreRecordsService(mockRouterConnector, mockRecordsRepository, mockRecordsSummaryRepository)
        val totalRecordsNum = 2000
        val requestEori     = "GB123456789099"

        when(mockRecordsRepository.getCountWithInactive(any())) thenReturn Future.successful(totalRecordsNum)
        when(mockRecordsSummaryRepository.set(any(), any())) thenReturn Future.successful(true)
        when(mockRecordsRepository.saveRecords(any(), any())) thenReturn Future.successful(true)
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
        val result = await(service.storeRecords(requestEori, None)(hc))

        eventually {
          verify(mockRouterConnector, times(5)).getRecords(any(), any(), any(), any())(any())
          verify(mockRecordsRepository, times(4)).saveRecords(any(), any())
          verify(mockRecordsSummaryRepository, times(5)).set(any(), any())
          verify(mockRecordsRepository).getCountWithInactive(any())
          result shouldBe false
        }
      }

      "must store all records in data-store with one page" in {
        val mockRouterConnector          = mock[RouterConnector]
        val mockRecordsRepository        = mock[RecordsRepository]
        val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]

        val service         = new StoreRecordsService(mockRouterConnector, mockRecordsRepository, mockRecordsSummaryRepository)
        val totalRecordsNum = recursivePageSize
        val requestEori     = "GB123456789099"

        when(mockRecordsRepository.getCountWithInactive(any())) thenReturn Future.successful(totalRecordsNum)

        when(mockRecordsSummaryRepository.set(any(), any())) thenReturn Future.successful(true)

        when(mockRecordsRepository.saveRecords(any(), any())) thenReturn Future.successful(true)

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
        verify(mockRecordsRepository, times(1)).saveRecords(any(), any())
        verify(mockRecordsRepository).getCountWithInactive(any())
      }

      "must store latest records in data-store with one page" in {
        val mockRouterConnector          = mock[RouterConnector]
        val mockRecordsRepository        = mock[RecordsRepository]
        val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]

        val service         = new StoreRecordsService(mockRouterConnector, mockRecordsRepository, mockRecordsSummaryRepository)
        val totalRecordsNum = recursivePageSize
        val requestEori     = "GB123456789099"

        when(mockRecordsRepository.getCountWithInactive(any())) thenReturn Future.successful(totalRecordsNum)

        when(mockRecordsSummaryRepository.set(any(), any())) thenReturn Future.successful(true)

        when(mockRecordsRepository.saveRecords(any(), any())) thenReturn Future.successful(true)

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
        verify(mockRecordsRepository, times(1)).saveRecords(any(), any())
        verify(mockRecordsRepository).getCountWithInactive(any())
      }
    }

    "deleteAndStoreRecords" - {
      "should wipe the db and store all records in data-store" in {
        val mockRouterConnector          = mock[RouterConnector]
        val mockRecordsRepository        = mock[RecordsRepository]
        val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]

        val service         = new StoreRecordsService(mockRouterConnector, mockRecordsRepository, mockRecordsSummaryRepository)
        val totalRecordsNum = 3000
        val requestEori     = "GB123456789099"

        when(mockRecordsRepository.deleteMany(any())) thenReturn Future.successful(10)
        when(mockRecordsSummaryRepository.set(any(), any())) thenReturn Future.successful(true)
        when(mockRecordsRepository.saveRecords(any(), any())) thenReturn Future.successful(true)
        when(mockRouterConnector.getRecords(any(), any(), any(), any())(any())) thenReturn (
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = getTestRecords(requestEori, recursivePageSize),
              Pagination(totalRecordsNum, 1, 6, Some(2), None)
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 2, 6, Some(3), Some(1))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 3, 6, Some(4), Some(2))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 4, 6, Some(5), Some(3))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 5, 6, Some(6), Some(4))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 6, 6, None, Some(5))
            )
          )
        )

        val result = await(service.deleteAndStoreRecords(requestEori)(hc))

        result.value shouldBe Done
        verify(mockRouterConnector, times(6)).getRecords(any(), any(), any(), any())(any())
        verify(mockRecordsRepository, times(6)).saveRecords(any(), any())
        verify(mockRecordsRepository).deleteMany(any())
      }
    }
  }
}
