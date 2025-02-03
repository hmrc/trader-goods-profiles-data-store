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
import org.mockito.Mockito._

import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class RecordsSummaryControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "recordsSummary" - {
    "return 200 and the RecordsSummary if it exists" in {

      val requestEori            = "eori"
      lazy val recordsSummaryUrl = routes.RecordsSummaryController.recordsSummary.url

      lazy val validFakeGetRequest = FakeRequest("GET", recordsSummaryUrl)
      val recordsSummary           = RecordsSummary(requestEori, None, lastUpdated = Instant.now)

      val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]
      when(mockRecordsSummaryRepository.get(any())) thenReturn Future.successful(
        Some(recordsSummary)
      )

      val application = applicationBuilder()
        .overrides(
          bind[RecordsSummaryRepository].toInstance(mockRecordsSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsJson(result) mustEqual Json.toJson(recordsSummary)
      }

      verify(mockRecordsSummaryRepository, atLeastOnce()).get(requestEori)
    }

    "return 404 if the RecordsSummary is not present" in {

      val requestEori            = "eori"
      lazy val recordsSummaryUrl = routes.RecordsSummaryController.recordsSummary.url

      lazy val validFakeGetRequest = FakeRequest("GET", recordsSummaryUrl)

      val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]
      when(mockRecordsSummaryRepository.get(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[RecordsSummaryRepository].toInstance(mockRecordsSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.NOT_FOUND
      }

      verify(mockRecordsSummaryRepository, times(1)).get(requestEori)
    }
  }
}
