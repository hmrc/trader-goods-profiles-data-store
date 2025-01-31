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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{atLeastOnce, times, verify, when}


import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeIdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import scala.concurrent.{ExecutionContext, Future}

class GetRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "getLocalRecords" - {
    "return 200 and the records from the data store with size 10 and page 1 and 35 records in db" in {

      val recordsSize = 10
      val page        = 1
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 35
      val totalPagesNum         = 4
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, Some(page + 1), None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, times(1)).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, times(1)).getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return 200 and the records from the data store with size 10 and page 1 and 10 records in db" in {

      val recordsSize = 10
      val page        = 1
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 10
      val totalPagesNum         = 1
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, None, None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, atLeastOnce()).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, atLeastOnce())
          .getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return 200 and the records from the data store with size 10 and page 1 and 1 record in db" in {

      val recordsSize = 10
      val page        = 1
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 1
      val totalPagesNum         = 1
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, None, None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, atLeastOnce()).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, atLeastOnce())
          .getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return 200 and the records from the data store with size 10 and page 1 and 0 records in db" in {

      val recordsSize = 10
      val page        = 1
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 0
      val totalPagesNum         = 1
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, None, None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, atLeastOnce()).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, atLeastOnce())
          .getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return 200 and the records from the data store with size 10 and page 1 and 20 records in db" in {

      val recordsSize = 10
      val page        = 1
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 20
      val totalPagesNum         = 2
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, Some(2), None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, atLeastOnce()).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, atLeastOnce())
          .getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return 200 and the records from the data store with size 10 and page 2 and 20 records in db" in {

      val recordsSize = 10
      val page        = 2
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 20
      val totalPagesNum         = 2
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, None, Some(1))
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, atLeastOnce()).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, atLeastOnce())
          .getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return 200 and the records from the data store with size 10 and page 3 and 21 records in db" in {

      val recordsSize = 10
      val page        = 3
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 21
      val totalPagesNum         = 3
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, None, Some(2))
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, atLeastOnce()).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, atLeastOnce())
          .getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return 200 and the records from the data store with size 10 and page 1 and 9 records in db" in {

      val recordsSize = 10
      val page        = 1
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 9
      val totalPagesNum         = 1
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, None, None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, atLeastOnce()).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, atLeastOnce())
          .getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return 200 and empty Seq from the data store with size 10 and page 100 and 35 records in db" in {

      val recordsSize = 10
      val page        = 100
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 35
      val totalPagesNum         = 4
      val records               = Seq.empty
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, None, None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString

        verify(mockRecordsRepository, atLeastOnce()).getCount(eqTo(requestEori))
        verify(mockRecordsRepository, atLeastOnce())
          .getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

      }
    }

    "return Runtime Error with size 10 and page -10 and 35 records in db" in {

      val recordsSize = 10
      val page        = -10
      val requestEori = "eori"
      val getUrl      = routes.GetRecordsController
        .getLocalRecords(Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 35
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any())) thenReturn Future.failed(
        new RuntimeException("Skip cannot be negative")
      )

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakeGetRequest).value)

          verify(mockRecordsRepository, times(1)).getCount(eqTo(requestEori))
          verify(mockRecordsRepository, times(1)).getMany(eqTo(requestEori), eqTo(Some(page)), eqTo(Some(recordsSize)))

        }
      }
    }
  }

  "getRecord" - {

    "return Ok with record in body when it is found" in {
      val requestEori   = "eori"
      val record        = getGoodsItemRecord(requestEori)
      val getRecordsUrl = routes.GetRecordsController
        .getRecord(record.recordId)
        .url

      val validFakeGetRequest = FakeRequest("GET", getRecordsUrl)

      val mockRouterConnector = mock[RouterConnector]

      when(mockRouterConnector.getRecord(any(), any())(any())) thenReturn Future.successful(Some(record))

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsString(result) mustBe Json.toJson(record).toString

        verify(mockRouterConnector, times(1)).getRecord(eqTo(requestEori), eqTo(record.recordId))(any())
      }
    }

    "return 404 when the record is inactive" in {
      val requestEori   = "eori"
      val record        = getGoodsInactiveItemRecord(requestEori)
      val getRecordsUrl = routes.GetRecordsController
        .getRecord(record.recordId)
        .url

      val validFakeGetRequest = FakeRequest("GET", getRecordsUrl)

      val mockRouterConnector = mock[RouterConnector]

      when(mockRouterConnector.getRecord(any(), any())(any())) thenReturn Future.successful(Some(record))

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.NOT_FOUND

        verify(mockRouterConnector, times(1)).getRecord(eqTo(requestEori), eqTo(record.recordId))(any())
      }
    }

    "return 404 when record does not exist because it has never existed" in {
      val requestEori   = "eori"
      val record        = getGoodsInactiveItemRecord(requestEori)
      val getRecordsUrl = routes.GetRecordsController
        .getRecord(record.recordId)
        .url

      val validFakeGetRequest = FakeRequest("GET", getRecordsUrl)
      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.getRecord(any(), any())(any())
      ) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe NOT_FOUND

        verify(mockRouterConnector, times(1)).getRecord(eqTo(requestEori), eqTo(record.recordId))(any())
      }
    }
  }
}
