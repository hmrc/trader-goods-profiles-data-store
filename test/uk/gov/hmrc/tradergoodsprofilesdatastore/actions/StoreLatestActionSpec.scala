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

import play.api.http.Status.INTERNAL_SERVER_ERROR
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.StoreLatestActionImpl
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.StoreRecordsService
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreLatestActionSpec extends SpecBase with GetRecordsResponseUtil {

  class Harness(recordsRepository: RecordsRepository, storeRecordsService: StoreRecordsService)
      extends StoreLatestActionImpl(recordsRepository, storeRecordsService) {
    def callFilter[A](request: IdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  "StoreLatestAction" - {

    "must store latest records" in {

      val requestEori = "GB123456789099"

      val mockRecordsRepository   = mock[RecordsRepository]
      when(mockRecordsRepository.getLatest(any())) thenReturn Future.successful(Some(getGoodsItemRecord(requestEori)))
      val mockStoreRecordsService = mock[StoreRecordsService]
      when(mockStoreRecordsService.storeRecords(any(), any())(any(), any())) thenReturn Future.successful(Done)

      val action = new Harness(mockRecordsRepository, mockStoreRecordsService)

      action
        .callFilter(IdentifierRequest(FakeRequest(), "testUserId", requestEori, AffinityGroup.Individual))
        .futureValue

      verify(mockRecordsRepository).getLatest(any())
      verify(mockStoreRecordsService).storeRecords(any(), any())(any(), any())
    }

    "must store all when there is no latest" in {

      val requestEori = "GB123456789099"

      val mockRecordsRepository   = mock[RecordsRepository]
      when(mockRecordsRepository.getLatest(any())) thenReturn Future.successful(None)
      val mockStoreRecordsService = mock[StoreRecordsService]
      when(mockStoreRecordsService.storeRecords(any(), any())(any(), any())) thenReturn Future.successful(Done)

      val action = new Harness(mockRecordsRepository, mockStoreRecordsService)

      action
        .callFilter(IdentifierRequest(FakeRequest(), "testUserId", requestEori, AffinityGroup.Individual))
        .futureValue

      verify(mockRecordsRepository).getLatest(any())
      verify(mockStoreRecordsService).storeRecords(any(), any())(any(), any())
    }

    "must throw error when there is a mismatch in the database" in {

      val requestEori = "GB123456789099"

      val mockRecordsRepository   = mock[RecordsRepository]
      when(mockRecordsRepository.getLatest(any())) thenReturn Future.successful(None)
      val mockStoreRecordsService = mock[StoreRecordsService]
      when(mockStoreRecordsService.storeRecords(any(), any())(any(), any())) thenReturn Future.failed(
        new RuntimeException(
          "Data Store and B&T Database are out of sync"
        )
      )

      val action = new Harness(mockRecordsRepository, mockStoreRecordsService)

      val result = action
        .callFilter(IdentifierRequest(FakeRequest(), "testUserId", requestEori, AffinityGroup.Individual))
        .futureValue
        .value

      status(Future.successful(result)) mustEqual INTERNAL_SERVER_ERROR

      verify(mockRecordsRepository).getLatest(any())
      verify(mockStoreRecordsService).storeRecords(any(), any())(any(), any())
    }
  }
}
