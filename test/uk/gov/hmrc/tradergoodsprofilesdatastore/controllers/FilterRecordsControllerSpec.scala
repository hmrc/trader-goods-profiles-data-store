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
import org.scalatest.BeforeAndAfterEach
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

class FilterRecordsControllerSpec
    extends SpecBase
    with MockitoSugar
    with GetRecordsResponseUtil
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  var mockRecordsRepository: RecordsRepository           = _
  var mockStoreRecordsController: StoreRecordsController = _
  var mockRouterConnector: RouterConnector               = _
  var mockAction: Action[AnyContent]                     = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockRecordsRepository = mock[RecordsRepository]
    mockStoreRecordsController = mock[StoreRecordsController]
    mockRouterConnector = mock[RouterConnector]
    mockAction = mock[Action[AnyContent]]

    when(mockRecordsRepository.saveRecords(any())) thenReturn Future.successful(true)
    when(mockRecordsRepository.deleteInactive(any())) thenReturn Future.successful(0)
    when(mockRecordsRepository.getLatest(any())) thenReturn Future.successful(
      Some(getGoodsItemRecords("GB123456789099"))
    )

    when(mockRouterConnector.getRecords(any(), any(), any(), any())(any())) thenReturn
      Future.successful(
        GetRecordsResponse(
          goodsItemRecords = getTestRecords("GB123456789099", 10),
          Pagination(25, 1, 3, Some(2), None)
        )
      )

    when(mockStoreRecordsController.storeLatestRecords(anyString()))
      .thenReturn(mockAction)
    when(mockAction.apply(any[Request[AnyContent]]))
      .thenReturn(Future.successful(NoContent))
  }

  "filterLocalRecords" - {
    "return 200 and the paginated records from the data store" in {
      val recordsSize = 25
      val page        = 1
      val size        = 10
      val field       = "traderRef"
      val searchTerm  = "BAN001002"
      val requestEori = "GB123456789099"
      val getUrl      = routes.FilterRecordsController
        .filterLocalRecords(requestEori, Some(searchTerm), Some(field), Some(page), Some(size))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)
      val records             = getTestRecords(requestEori, recordsSize)
      val paginatedRecords    = records.slice(page * size, (page + 1) * size)
      val pagination          = Pagination(recordsSize, page, 3, Some(page + 1), Some(page - 1))

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
        contentAsString(result) mustBe Json
          .toJson(GetRecordsResponse(goodsItemRecords = paginatedRecords, pagination))
          .toString
      }
    }

    "return 400 when the field is not as expected" in {
      val page        = 1
      val size        = 10
      val field       = "trader"
      val searchTerm  = "BAN001002"
      val requestEori = "GB123456789099"
      val getUrl      = routes.FilterRecordsController
        .filterLocalRecords(requestEori, Some(searchTerm), Some(field), Some(page), Some(size))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val records = getTestRecords(requestEori, 25)

      when(mockRecordsRepository.filterRecords(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector),
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

}
