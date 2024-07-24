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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.StoreRecordsService
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import scala.concurrent.{ExecutionContext, Future}

class StoreRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "storeAllRecords" - {

    "return 204 and store all records in db" in {
      val requestEori = "GB123456789099"
      val storeUrl    = routes.StoreRecordsController
        .storeAllRecords(requestEori)
        .url

      val validFakeHeadRequest = FakeRequest("HEAD", storeUrl)

      val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]
      when(mockRecordsSummaryRepository.set(any(), any(), any(), any())) thenReturn Future.successful(true)

      val mockStoreRecordsService = mock[StoreRecordsService]
      when(mockStoreRecordsService.deleteAndStoreRecords(eqTo(requestEori))(any())) thenReturn Future
        .successful(Done)

      val application = applicationBuilder()
        .overrides(
          bind[StoreRecordsService].toInstance(mockStoreRecordsService),
          bind[RecordsSummaryRepository].toInstance(mockRecordsSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeHeadRequest).value
        status(result) shouldBe Status.NO_CONTENT
      }
    }
  }
}
