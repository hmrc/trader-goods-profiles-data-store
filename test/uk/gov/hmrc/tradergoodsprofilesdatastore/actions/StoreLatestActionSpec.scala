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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.StoreLatestActionImpl
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreLatestActionSpec extends SpecBase with GetRecordsResponseUtil {

  class Harness(recordsRepository: RecordsRepository, routerConnector: RouterConnector)
      extends StoreLatestActionImpl(recordsRepository, routerConnector) {
    def callFilter[A](request: IdentifierRequest[A]): Future[Option[Result]] = filter(request)
  }

  "StoreLatestAction" - {

    "must store latest records" in {

      val totalRecordsNum = 29
      val requestEori     = "GB123456789099"
      val recordsPerPage  = 10

      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.saveRecords(any(), any())) thenReturn Future.successful(true)
      when(mockRecordsRepository.deleteInactive(any())) thenReturn Future.successful(0)
      when(mockRecordsRepository.getLatest(any())) thenReturn Future.successful(Some(getGoodsItemRecords(requestEori)))
      val mockRouterConnector   = mock[RouterConnector]
      when(mockRouterConnector.getRecords(any(), any(), any(), any())(any())) thenReturn (
        Future.successful(
          GetRecordsResponse(
            goodsItemRecords = getTestRecords(requestEori, recordsPerPage),
            Pagination(totalRecordsNum, 1, 3, Some(2), None)
          )
        ),
        Future.successful(
          GetRecordsResponse(
            goodsItemRecords = getTestRecords(requestEori, recordsPerPage),
            Pagination(totalRecordsNum, 2, 3, Some(3), Some(1))
          )
        ),
        Future.successful(
          GetRecordsResponse(
            goodsItemRecords = getTestRecords(requestEori, 9),
            Pagination(totalRecordsNum, 3, 3, None, Some(2))
          )
        )
      )

      val action = new Harness(mockRecordsRepository, mockRouterConnector)

      action
        .callFilter(IdentifierRequest(FakeRequest(), "testUserId", requestEori, AffinityGroup.Individual))
        .futureValue

      verify(mockRecordsRepository, times(1)).getLatest(any())
      verify(mockRouterConnector, times(1)).getRecords(any(), any(), any(), any())(any())
      verify(mockRecordsRepository, times(1)).saveRecords(any(), any())
      verify(mockRecordsRepository, times(1)).deleteInactive(any())

    }
  }
}
