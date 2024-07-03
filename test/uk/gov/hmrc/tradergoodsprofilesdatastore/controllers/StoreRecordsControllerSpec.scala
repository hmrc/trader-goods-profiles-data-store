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

package uk.gov.hmrc.tradergoodsprofilesdatastore.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.CheckRecords
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{CheckRecordsRepository, RecordsRepository}
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import scala.concurrent.{ExecutionContext, Future}

class StoreRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "storeAllRecords" - {

    "return 200 and store all records in db" in {
      val totalRecordsNum = 29

      val requestEori          = "GB123456789099"
      val storeUrl             = routes.StoreRecordsController
        .storeAllRecords(requestEori)
        .url
      val recordsPerPage       = 10
      val validFakeHeadRequest = FakeRequest("HEAD", storeUrl)

      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.saveRecords(any())) thenReturn Future.successful(true)
      when(mockRecordsRepository.deleteInactive(any())) thenReturn Future.successful(0)

      val mockCheckRecordsRepository = mock[CheckRecordsRepository]
      when(mockCheckRecordsRepository.set(any())) thenReturn Future.successful(true)
      val mockRouterConnector        = mock[RouterConnector]
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

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector),
          bind[RecordsRepository].toInstance(mockRecordsRepository),
          bind[CheckRecordsRepository].toInstance(mockCheckRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeHeadRequest).value
        status(result) shouldBe Status.OK
        verify(mockCheckRecordsRepository, times(1)).set(any())
        verify(mockRouterConnector, times(3)).getRecords(any(), any(), any(), any())(any())
        verify(mockRecordsRepository, times(3)).saveRecords(any())
        verify(mockRecordsRepository, times(1)).deleteInactive(any())
      }
    }
  }

  "storeLatestRecords" - {

    "return 200 and store latest records in db" in {
      val totalRecordsNum = 29

      val requestEori          = "GB123456789099"
      val storeUrl             = routes.StoreRecordsController
        .storeLatestRecords(requestEori)
        .url
      val recordsPerPage       = 10
      val validFakeHeadRequest = FakeRequest("HEAD", storeUrl)

      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.saveRecords(any())) thenReturn Future.successful(true)
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

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector),
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeHeadRequest).value
        status(result) shouldBe Status.OK
        verify(mockRecordsRepository, times(1)).getLatest(any())
        verify(mockRouterConnector, times(3)).getRecords(any(), any(), any(), any())(any())
        verify(mockRecordsRepository, times(3)).saveRecords(any())
        verify(mockRecordsRepository, times(1)).deleteInactive(any())
      }
    }
  }
}
