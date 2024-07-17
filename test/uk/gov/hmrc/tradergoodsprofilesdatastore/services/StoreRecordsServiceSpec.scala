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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.recursivePageSize
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import scala.concurrent.{ExecutionContext, Future}

class StoreRecordsServiceSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  "StoreRecordsService" - {
    "storeRecords" - {
      "must store all records in data-store" in {
        val mockRouterConnector   = mock[RouterConnector]
        val mockRecordsRepository = mock[RecordsRepository]

        val service         = new StoreRecordsService(mockRouterConnector, mockRecordsRepository)
        val totalRecordsNum = 60000
        val requestEori     = "GB123456789099"

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
              Pagination(totalRecordsNum, 2, 6, Some(2), Some(1))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 3, 6, Some(2), Some(2))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 4, 6, Some(2), Some(3))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 5, 6, Some(2), Some(4))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 6, 6, None, Some(5))
            )
          ),
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = Seq(getGoodsItemRecord(requestEori)),
              Pagination(totalRecordsNum, 2, 2, None, Some(1))
            )
          )
        )

        val result = await(service.storeRecords(requestEori, None)(FakeRequest(), hc))

        result.value shouldBe Done
        verify(mockRouterConnector, times(6)).getRecords(any(), any(), any(), any())(any())
        verify(mockRecordsRepository, times(6)).saveRecords(any(), any())
      }

      "must store latest records in data-store" in {
        val mockRouterConnector   = mock[RouterConnector]
        val mockRecordsRepository = mock[RecordsRepository]

        val service         = new StoreRecordsService(mockRouterConnector, mockRecordsRepository)
        val totalRecordsNum = 11
        val requestEori     = "GB123456789099"

        when(mockRecordsRepository.saveRecords(any(), any())) thenReturn Future.successful(true)
        when(mockRouterConnector.getRecords(any(), any(), any(), any())(any())) thenReturn
          Future.successful(
            GetRecordsResponse(
              goodsItemRecords = getTestRecords(requestEori, totalRecordsNum),
              Pagination(totalRecordsNum, 1, 1, None, None)
            )
          )
        val result = await(service.storeRecords(requestEori, Some("2024-10-12T16:12:34Z"))(FakeRequest(), hc))

        result.value shouldBe Done
        verify(mockRouterConnector, times(1)).getRecords(any(), any(), any(), any())(any())
        verify(mockRecordsRepository, times(1)).saveRecords(any(), any())
      }
    }
  }
}
