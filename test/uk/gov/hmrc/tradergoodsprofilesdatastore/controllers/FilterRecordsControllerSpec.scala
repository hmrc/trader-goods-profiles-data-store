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

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Results.NoContent
import play.api.mvc.{Action, AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import scala.concurrent.{ExecutionContext, Future}

class FilterRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "filterLocalRecords" - {
    "return 200 and the filtered records from the data store when the search term matches" in {

      val recordsSize = 5
      val page        = 0
      val field       = "traderRef"
      val searchTerm  = "BAN001002"
      val requestEori = "GB123456789099"
      val getUrl      = routes.FilterRecordsController
        .filterLocalRecords(requestEori, Some(searchTerm), Some(field), None, None)
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalPagesNum              = 1
      val records                    = getTestRecords(requestEori, recordsSize)
      val pagination                 = Pagination(recordsSize, page, totalPagesNum, None, None)
      val recordsPerPage             = 10
      val mockRecordsRepository      = mock[RecordsRepository]
      val mockStoreRecordsController = mock[StoreRecordsController]

      when(mockRecordsRepository.saveRecords(any())) thenReturn Future.successful(true)
      when(mockRecordsRepository.deleteInactive(any())) thenReturn Future.successful(0)
      when(mockRecordsRepository.getLatest(any())) thenReturn Future.successful(Some(getGoodsItemRecords(requestEori)))
      val mockRouterConnector = mock[RouterConnector]
      when(mockRouterConnector.getRecords(any(), any(), any(), any())(any())) thenReturn (
        Future.successful(
          GetRecordsResponse(
            goodsItemRecords = getTestRecords(requestEori, recordsPerPage),
            Pagination(recordsSize, 1, 3, Some(2), None)
          )
        )
      )

      val mockAction = mock[Action[AnyContent]]
      when(mockStoreRecordsController.storeLatestRecords(anyString()))
        .thenReturn(mockAction)
      when(mockAction.apply(any[Request[AnyContent]]))
        .thenReturn(Future.successful(NoContent))

      when(mockRecordsRepository.filterRecords(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector),
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString
      }
    }
  }

}
