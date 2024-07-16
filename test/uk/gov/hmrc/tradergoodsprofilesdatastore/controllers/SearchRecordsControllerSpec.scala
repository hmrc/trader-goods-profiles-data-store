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
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import scala.concurrent.{ExecutionContext, Future}

class SearchRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global
  val searchString                  = "search value"

  "getLocalRecords" - {
    "return 200 and the records from the data store with size 10 and page 1 and 35 records in db" in {

      val recordsSize = 10
      val page        = 1
      val requestEori = "GB123456789099"
      val getUrl      = routes.SearchRecordsController
        .getLocalRecords(requestEori, searchString, Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 35
      val totalPagesNum         = 4
      val records               = getTestRecords(requestEori, recordsSize)
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, Some(page + 1), None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any(), any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString
      }
    }

    "return 200 and empty Seq from the data store with size 10 and page 100 and 35 records in db" in {

      val recordsSize = 10
      val page        = 100
      val requestEori = "GB123456789099"
      val getUrl      = routes.SearchRecordsController
        .getLocalRecords(requestEori, searchString, Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 35
      val totalPagesNum         = 4
      val records               = Seq.empty
      val pagination            = Pagination(totalRecordsNum, page, totalPagesNum, None, None)
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any(), any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any(), any())) thenReturn Future.successful(records)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK
        contentAsString(result) mustBe Json.toJson(GetRecordsResponse(goodsItemRecords = records, pagination)).toString
      }
    }

    "return Runtime Error with size 10 and page -10 and 35 records in db" in {

      val recordsSize = 10
      val page        = -10
      val requestEori = "GB123456789099"

      val getUrl = routes.SearchRecordsController
        .getLocalRecords(requestEori, searchString, Some(page), Some(recordsSize))
        .url

      val validFakeGetRequest = FakeRequest("GET", getUrl)

      val totalRecordsNum       = 35
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.getCount(any(), any())) thenReturn Future.successful(totalRecordsNum)
      when(mockRecordsRepository.getMany(any(), any(), any(), any())) thenReturn Future.failed(
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
        }
      }
    }
  }
}
