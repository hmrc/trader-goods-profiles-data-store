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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.CheckRecords
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.CheckRecordsRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CheckRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "checkRecords" - {
    "return 204 if the checkRecords is present" in {

      val requestEori = "GB123456789099"
      val checkUrl    = routes.CheckRecordsController
        .checkRecords(requestEori)
        .url

      val validFakeGetRequest = FakeRequest("HEAD", checkUrl)

      val mockCheckRecordsRepository = mock[CheckRecordsRepository]
      when(mockCheckRecordsRepository.get(any())) thenReturn Future.successful(
        Some(CheckRecords(requestEori, recordsUpdating = false, lastUpdated = Instant.now))
      )

      val application = applicationBuilder()
        .overrides(
          bind[CheckRecordsRepository].toInstance(mockCheckRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.NO_CONTENT
      }
    }

    "return 404 if the checkRecords is not present" in {

      val requestEori = "GB123456789099"
      val checkUrl    = routes.CheckRecordsController
        .checkRecords(requestEori)
        .url

      val validFakeGetRequest = FakeRequest("HEAD", checkUrl)

      val mockCheckRecordsRepository = mock[CheckRecordsRepository]
      when(mockCheckRecordsRepository.get(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[CheckRecordsRepository].toInstance(mockCheckRecordsRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }
    }

  }

}
