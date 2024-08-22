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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReady}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository

import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "getDownloadDataSummary" - {

    "return 200 and the DownloadDataSummary if it exists" in {

      val requestEori                 = "GB123456789099"
      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .getDownloadDataSummary(requestEori)
        .url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataSummaryUrl)
      val downloadDataSummary      = DownloadDataSummary(requestEori, FileInProgress)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(
        Some(downloadDataSummary)
      )

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK
        contentAsJson(result) mustEqual Json.toJson(downloadDataSummary)
      }

      verify(mockDownloadDataSummaryRepository, times(1)).get(requestEori)
    }

    "return 404 if the DownloadDataSummary is not present" in {

      val requestEori                 = "GB123456789099"
      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .getDownloadDataSummary(requestEori)
        .url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataSummaryUrl)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }

      verify(mockDownloadDataSummaryRepository, times(1)).get(requestEori)
    }
  }

  "requestDownloadData" - {

    "return 202 when data is successfully requested" in {
      val requestEori         = "GB123456789099"
      val downloadDataSummary = DownloadDataSummary(requestEori, FileInProgress)

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .requestDownloadData(requestEori)
        .url
      lazy val validFakePostRequest   = FakeRequest("POST", downloadDataSummaryUrl)

      val mockRouterConnector = mock[RouterConnector]
      when(
        mockRouterConnector.getRequestDownloadData(any())(any())
      ) thenReturn Future.successful(Done)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.set(any())
      ) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector),
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()

      running(application) {
        val request = validFakePostRequest
          .withHeaders("Content-Type" -> "application/json")
        val result  = route(application, request).value
        status(result) shouldBe ACCEPTED

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector)
            .getRequestDownloadData(eqTo(requestEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .set(eqTo(downloadDataSummary))
        }
      }
    }
  }

  "updateDownloadDataStatus" - {

    "return 204 when data is successfully updated" in {
      val requestEori         = "GB123456789099"
      val downloadDataSummary = DownloadDataSummary(requestEori, FileReady)

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .updateDownloadDataStatus(requestEori)
        .url
      lazy val validFakePostRequest   = FakeRequest("PATCH", downloadDataSummaryUrl)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.update(any(), any())
      ) thenReturn Future.successful(true)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()

      running(application) {
        val request = validFakePostRequest
          .withHeaders("Content-Type" -> "application/json")
          .withJsonBody(Json.toJson(downloadDataSummary))
        val result  = route(application, request).value
        status(result) shouldBe NO_CONTENT

        withClue("must call the relevant services with the correct details") {
          verify(mockDownloadDataSummaryRepository)
            .update(eqTo(downloadDataSummary.eori), eqTo(downloadDataSummary.status))
        }
      }
    }

    "return 404 when summary is not found so cannot be updated" in {
      val requestEori         = "GB123456789099"
      val downloadDataSummary = DownloadDataSummary(requestEori, FileReady)

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .updateDownloadDataStatus(requestEori)
        .url
      lazy val validFakePostRequest   = FakeRequest("PATCH", downloadDataSummaryUrl)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.update(any(), any())
      ) thenReturn Future.successful(false)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()

      running(application) {
        val request = validFakePostRequest
          .withHeaders("Content-Type" -> "application/json")
          .withJsonBody(Json.toJson(downloadDataSummary))
        val result  = route(application, request).value
        status(result) shouldBe NOT_FOUND

        withClue("must call the relevant services with the correct details") {
          verify(mockDownloadDataSummaryRepository)
            .update(eqTo(downloadDataSummary.eori), eqTo(downloadDataSummary.status))
        }
      }
    }

  }
}
