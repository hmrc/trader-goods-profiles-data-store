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
import org.mockito.Mockito.{atLeastOnce, never, verify, when}

import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.inject.bind
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.TestConstants.testEori
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{CorrelationId, DownloadData, DownloadDataNotification, Metadata}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.SdesService

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val summaryId = java.util.UUID.randomUUID().toString
  private val now       = Instant.parse("2018-11-30T18:35:24.00Z")

  private val clock = Clock.fixed(now, ZoneOffset.UTC)

  "getDownloadDataSummaries" - {

    "return 200 and the DownloadDataSummaries" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController.getDownloadDataSummaries.url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataSummaryUrl)
      val downloadDataSummary      = DownloadDataSummary(summaryId, testEori, FileInProgress, now, now, None)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(
        Seq(downloadDataSummary)
      )

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[Clock].toInstance(clock)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsJson(result) mustEqual Json.toJson(Seq(downloadDataSummary))
      }

      verify(mockDownloadDataSummaryRepository, atLeastOnce()).get(testEori)
    }

    "return 200 and empty list if the DownloadDataSummaries are not present" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController.getDownloadDataSummaries.url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataSummaryUrl)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(Seq.empty)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsJson(result) mustEqual JsArray()
      }

      verify(mockDownloadDataSummaryRepository, atLeastOnce()).get(testEori)
    }
  }

  "touchDownloadDataSummaries" - {

    "return 204 and update summaries" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController.touchDownloadDataSummaries.url

      lazy val validFakePatchRequest = FakeRequest("PATCH", downloadDataSummaryUrl)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.updateSeen(any())) thenReturn Future.successful(1L)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[Clock].toInstance(clock)
        )
        .build()
      running(application) {
        val result = route(application, validFakePatchRequest).value
        status(result) mustBe Status.NO_CONTENT
      }

      verify(mockDownloadDataSummaryRepository).updateSeen(testEori)
    }

    "return 200 and empty list if the DownloadDataSummaries are not present" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController.getDownloadDataSummaries.url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataSummaryUrl)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.get(any())) thenReturn Future.successful(Seq.empty)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe Status.OK
        contentAsJson(result) mustEqual JsArray()
      }

      verify(mockDownloadDataSummaryRepository).get(testEori)
    }
  }

  "submitNotification" - {

    "return 204 if the notification is submitted successfully" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val oldSummary = DownloadDataSummary(
        summaryId,
        testEori,
        FileInProgress,
        now,
        now.plus(30, ChronoUnit.DAYS),
        None
      )

      val newSummary = DownloadDataSummary(
        summaryId,
        testEori,
        FileReadyUnseen,
        now,
        now.plus(retentionDays.toInt, ChronoUnit.DAYS),
        Some(FileInfo(fileName, fileSize, retentionDays))
      )

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest         = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))
        .withHeaders("x-conversation-id" -> summaryId)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockSdesService                   = mock[SdesService]

      when(mockDownloadDataSummaryRepository.get(any(), any())) thenReturn Future.successful(Some(oldSummary))
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockSdesService.enqueueSubmission(any())) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[SdesService].toInstance(mockSdesService),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) mustBe Status.NO_CONTENT
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).get(eqTo(testEori), eqTo(oldSummary.summaryId))
        verify(mockDownloadDataSummaryRepository).set(eqTo(newSummary))
        verify(mockSdesService)
          .enqueueSubmission(eqTo(newSummary))
      }
    }

    "return 204 if the notification is submitted successfully with the flag false so nothing is queued" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val oldSummary = DownloadDataSummary(
        summaryId,
        testEori,
        FileInProgress,
        now,
        now.plus(30, ChronoUnit.DAYS),
        None
      )

      val newSummary = DownloadDataSummary(
        summaryId,
        testEori,
        FileReadyUnseen,
        now,
        now.plus(retentionDays.toInt, ChronoUnit.DAYS),
        Some(FileInfo(fileName, fileSize, retentionDays))
      )

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest         = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))
        .withHeaders("x-conversation-id" -> summaryId)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockSdesService                   = mock[SdesService]
      val mockDataStoreAppConfig            = mock[DataStoreAppConfig]

      when(mockDownloadDataSummaryRepository.get(any(), any())) thenReturn Future.successful(Some(oldSummary))
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockDataStoreAppConfig.sendNotificationEmail) thenReturn false

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[SdesService].toInstance(mockSdesService),
          bind[DataStoreAppConfig].toInstance(mockDataStoreAppConfig),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) mustBe Status.NO_CONTENT
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).get(eqTo(testEori), eqTo(oldSummary.summaryId))
        verify(mockDownloadDataSummaryRepository).set(eqTo(newSummary))
        verify(mockDataStoreAppConfig).sendNotificationEmail
        verify(mockSdesService, never)
          .enqueueSubmission(any())
      }
    }

    "return error if email submission is not queued" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val oldSummary = DownloadDataSummary(
        summaryId,
        testEori,
        FileInProgress,
        now,
        now.plus(30, ChronoUnit.DAYS),
        None
      )

      val newSummary = DownloadDataSummary(
        summaryId,
        testEori,
        FileReadyUnseen,
        now,
        now.plus(retentionDays.toInt, ChronoUnit.DAYS),
        Some(FileInfo(fileName, fileSize, retentionDays))
      )

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest         = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))
        .withHeaders("x-conversation-id" -> summaryId)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockSdesService                   = mock[SdesService]

      when(mockDownloadDataSummaryRepository.get(any(), any())) thenReturn Future.successful(Some(oldSummary))
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockSdesService.enqueueSubmission(any())) thenReturn Future.failed(
        new RuntimeException("Work not queued!")
      )

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[SdesService].toInstance(mockSdesService),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).get(eqTo(testEori), eqTo(oldSummary.summaryId))
        verify(mockDownloadDataSummaryRepository).set(eqTo(newSummary))
        verify(mockSdesService)
          .enqueueSubmission(eqTo(newSummary))
      }
    }

    "return error if matching summary not found" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "hello"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest         = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))
        .withHeaders("x-conversation-id" -> summaryId)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockSdesService                   = mock[SdesService]

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[SdesService].toInstance(mockSdesService),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository, never).get(any(), any())
        verify(mockDownloadDataSummaryRepository, never).set(any())
        verify(mockSdesService, never).enqueueSubmission(any())
      }
    }

    "return error if retention days not a number" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest         = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))
        .withHeaders("x-conversation-id" -> summaryId)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockSdesService                   = mock[SdesService]

      when(mockDownloadDataSummaryRepository.get(any(), any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[SdesService].toInstance(mockSdesService),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).get(eqTo(testEori), eqTo(summaryId))
        verify(mockDownloadDataSummaryRepository, never).set(any())
        verify(mockSdesService, never).enqueueSubmission(any())
      }
    }

    "return error if retention days not found" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName = "fileName"
      val fileSize = 600

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq.empty)

      lazy val validFakePostRequest         = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))
        .withHeaders("x-conversation-id" -> summaryId)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockSdesService                   = mock[SdesService]

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[SdesService].toInstance(mockSdesService),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository, never).get(any(), any())
        verify(mockDownloadDataSummaryRepository, never).set(any())
        verify(mockSdesService, never).enqueueSubmission(any())
      }
    }

    "return error if x-conversation-id not found in the header" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockSdesService                   = mock[SdesService]

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[SdesService].toInstance(mockSdesService),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository, never).get(any(), any())
        verify(mockDownloadDataSummaryRepository, never).set(any())
        verify(mockSdesService, never).enqueueSubmission(any())
      }
    }

  }

  "requestDownloadData" - {

    "return 202 when data is successfully requested" in {

      val correlationId = CorrelationId(summaryId)

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController.requestDownloadData.url
      lazy val validFakePostRequest   = FakeRequest("POST", downloadDataSummaryUrl)

      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.getRequestDownloadData(any())(any())
      ) thenReturn Future.successful(correlationId)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.set(any())
      ) thenReturn Future.successful(Done)

      val summary = DownloadDataSummary(
        summaryId,
        testEori,
        FileInProgress,
        now,
        now.plus(30, ChronoUnit.DAYS),
        None
      )

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector),
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        val request = validFakePostRequest
          .withHeaders("Content-Type" -> "application/json")
        val result  = route(application, request).value
        status(result) mustBe ACCEPTED

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector).getRequestDownloadData(eqTo(testEori))(any())
          verify(mockDownloadDataSummaryRepository).set(eqTo(summary))
        }
      }
    }
  }

  "getDownloadData" - {

    "return 200 when download data is successfully requested" in {

      val fileName      = "fileName"
      val fileSize      = 600
      val retentionDays = "30"

      val url                      = "/some-url"
      val fileRoleMetadata         = Metadata("FileRole", "C79Certificate")
      val periodStartYearMetadata  = Metadata("PeriodStartYear", "2020")
      val retentionDaysMetadata    = Metadata("RETENTION_DAYS", retentionDays)
      val periodStartMonthMetadata = Metadata("PeriodStartMonth", "08")

      val downloadData = DownloadData(
        url,
        fileName,
        fileSize,
        Seq(
          fileRoleMetadata,
          periodStartYearMetadata,
          retentionDaysMetadata,
          periodStartMonthMetadata
        )
      )

      lazy val downloadDataUrl = routes.DownloadDataSummaryController.getDownloadData.url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataUrl)

      val mockSecureDataExchangeProxyConnector = mock[SecureDataExchangeProxyConnector]
      when(
        mockSecureDataExchangeProxyConnector.getFilesAvailableUrl(any())(any())
      ) thenReturn Future.successful(Seq(downloadData))

      val application = applicationBuilder()
        .overrides(
          bind[SecureDataExchangeProxyConnector].toInstance(mockSecureDataExchangeProxyConnector)
        )
        .build()

      running(application) {
        val request = validFakeGetRequest
          .withHeaders("Content-Type" -> "application/json")
        val result  = route(application, request).value
        status(result) mustBe OK
        contentAsJson(result) mustEqual Json.toJson(Seq(downloadData))

        withClue("must call the relevant services with the correct details") {
          verify(mockSecureDataExchangeProxyConnector)
            .getFilesAvailableUrl(eqTo(testEori))(any())
        }
      }
    }
  }
}
