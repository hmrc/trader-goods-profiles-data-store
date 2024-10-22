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
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.TestConstants.testEori
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{DownloadData, DownloadDataNotification, Email, Metadata}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.DownloadRecordEmailParameters
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.UuidService

import java.time.{Clock, Instant, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val appConfig = mock[DataStoreAppConfig]
  private val id        = java.util.UUID.randomUUID().toString
  private val now       = Instant.parse("2018-11-30T18:35:24.00Z")

  private val clock = Clock.fixed(now, ZoneOffset.UTC)

  "getDownloadDataSummaries" - {

    "return 200 and the DownloadDataSummaries" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .getDownloadDataSummaries(testEori)
        .url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataSummaryUrl)
      val downloadDataSummary      = DownloadDataSummary(id, testEori, FileInProgress, now, now, None)

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
        status(result) shouldBe Status.OK
        contentAsJson(result) mustEqual Json.toJson(Seq(downloadDataSummary))
      }

      verify(mockDownloadDataSummaryRepository).get(testEori)
    }

    "return 200 and empty list if the DownloadDataSummaries are not present" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .getDownloadDataSummaries(testEori)
        .url

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
        status(result) shouldBe Status.OK
        contentAsJson(result) mustEqual JsArray()
      }

      verify(mockDownloadDataSummaryRepository).get(testEori)
    }
  }

  "submitNotification" - {

    val timestamp = Instant.now
    val address   = "email@test.co.uk"
    val email     = Email(address, timestamp)

    "return 204 if the notification is submitted successfully" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val oldSummary = DownloadDataSummary(
        id,
        testEori,
        FileInProgress,
        now,
        now.plus(30, ChronoUnit.DAYS),
        None
      )

      val newSummary = DownloadDataSummary(
        id,
        testEori,
        FileReadyUnseen,
        now,
        now.plus(retentionDays.toInt, ChronoUnit.DAYS),
        Some(FileInfo(fileName, fileSize, retentionDays))
      )

      val downloadRecordEmailParameters = DownloadRecordEmailParameters(
        "30 December 2018"
      )

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.getOldestInProgress(any())) thenReturn Future.successful(Some(oldSummary))
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(Some(email))
      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any())) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
          bind[EmailConnector].toInstance(mockEmailConnector),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.NO_CONTENT
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).getOldestInProgress(eqTo(testEori))
        verify(mockDownloadDataSummaryRepository).set(eqTo(newSummary))
        verify(mockCustomsDataStoreConnector).getEmail(eqTo(testEori))(any())
        verify(mockEmailConnector).sendDownloadRecordEmail(eqTo(email.address), eqTo(downloadRecordEmailParameters))(
          any()
        )
      }
    }

    "return 204 if the notification is submitted successfully but no email is sent because flag is false" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val oldSummary = DownloadDataSummary(
        id,
        testEori,
        FileInProgress,
        now,
        now.plus(30, ChronoUnit.DAYS),
        None
      )

      val newSummary = DownloadDataSummary(
        id,
        testEori,
        FileReadyUnseen,
        now,
        now.plus(retentionDays.toInt, ChronoUnit.DAYS),
        Some(FileInfo(fileName, fileSize, retentionDays))
      )

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.getOldestInProgress(any())) thenReturn Future.successful(Some(oldSummary))
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(appConfig.sendNotificationEmail) thenReturn false

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
          bind[EmailConnector].toInstance(mockEmailConnector),
          bind[Clock].toInstance(clock),
          bind[DataStoreAppConfig].toInstance(appConfig)
        )
        .build()

      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.NO_CONTENT
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).getOldestInProgress(eqTo(testEori))
        verify(mockDownloadDataSummaryRepository).set(eqTo(newSummary))
        verify(mockCustomsDataStoreConnector, never).getEmail(any())(any())
        verify(mockEmailConnector, never).sendDownloadRecordEmail(any(), any())(any())
      }
    }

    "return 404 if email not found" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val oldSummary = DownloadDataSummary(
        id,
        testEori,
        FileInProgress,
        now,
        now.plus(30, ChronoUnit.DAYS),
        None
      )

      val newSummary = DownloadDataSummary(
        id,
        testEori,
        FileReadyUnseen,
        now,
        now.plus(retentionDays.toInt, ChronoUnit.DAYS),
        Some(FileInfo(fileName, fileSize, retentionDays))
      )

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.getOldestInProgress(any())) thenReturn Future.successful(Some(oldSummary))
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
          bind[EmailConnector].toInstance(mockEmailConnector),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).getOldestInProgress(eqTo(testEori))
        verify(mockDownloadDataSummaryRepository).set(eqTo(newSummary))
        verify(mockCustomsDataStoreConnector).getEmail(eqTo(testEori))(any())
        verify(mockEmailConnector, never()).sendDownloadRecordEmail(any(), any())(
          any()
        )
      }
    }

    "return error if matching summary not found" in {

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
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.getOldestInProgress(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
          bind[EmailConnector].toInstance(mockEmailConnector),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).getOldestInProgress(eqTo(testEori))
        verify(mockDownloadDataSummaryRepository, never).set(any())
        verify(mockCustomsDataStoreConnector, never).getEmail(any())(any())
        verify(mockEmailConnector, never).sendDownloadRecordEmail(any(), any())(any())
      }
    }

    "return error if EmailConnector fails" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDays         = "30"
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", retentionDays)

      val oldSummary = DownloadDataSummary(
        id,
        testEori,
        FileInProgress,
        now,
        now.plus(30, ChronoUnit.DAYS),
        None
      )

      val newSummary = DownloadDataSummary(
        id,
        testEori,
        FileReadyUnseen,
        now,
        now.plus(retentionDays.toInt, ChronoUnit.DAYS),
        Some(FileInfo(fileName, fileSize, retentionDays))
      )

      val downloadRecordEmailParameters = DownloadRecordEmailParameters(
        "30 December 2018"
      )

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.getOldestInProgress(any())) thenReturn Future.successful(Some(oldSummary))
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(Some(email))
      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any())) thenReturn Future.failed(
        new RuntimeException("Failed to send the email")
      )

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
          bind[EmailConnector].toInstance(mockEmailConnector),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }

      withClue("must call the relevant services with the correct details") {
        verify(mockDownloadDataSummaryRepository).getOldestInProgress(eqTo(testEori))
        verify(mockDownloadDataSummaryRepository).set(eqTo(newSummary))
        verify(mockCustomsDataStoreConnector).getEmail(eqTo(testEori))(any())
        verify(mockEmailConnector).sendDownloadRecordEmail(eqTo(email.address), eqTo(downloadRecordEmailParameters))(
          any()
        )
      }
    }

    "return error if retention days not found" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName         = "fileName"
      val fileSize         = 600
      val filetypeMetaData = Metadata("FILETYPE", "csv")

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(filetypeMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val application = applicationBuilder()
        .build()
      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }
    }
  }

  "requestDownloadData" - {

    "return 202 when data is successfully requested" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .requestDownloadData(testEori)
        .url
      lazy val validFakePostRequest   = FakeRequest("POST", downloadDataSummaryUrl)

      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.getRequestDownloadData(any())(any())
      ) thenReturn Future.successful(Done)

      val mockUuidService = mock[UuidService]
      when(
        mockUuidService.generate()
      ) thenReturn id

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.set(any())
      ) thenReturn Future.successful(Done)

      val summary = DownloadDataSummary(
        id,
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
          bind[UuidService].toInstance(mockUuidService),
          bind[Clock].toInstance(clock)
        )
        .build()

      running(application) {
        val request = validFakePostRequest
          .withHeaders("Content-Type" -> "application/json")
        val result  = route(application, request).value
        status(result) shouldBe ACCEPTED

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector).getRequestDownloadData(eqTo(testEori))(any())
          verify(mockDownloadDataSummaryRepository).set(eqTo(summary))
          verify(mockUuidService).generate()
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

      lazy val downloadDataUrl = routes.DownloadDataSummaryController
        .getDownloadData(testEori)
        .url

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
        status(result) shouldBe OK
        contentAsJson(result) mustEqual Json.toJson(Seq(downloadData))

        withClue("must call the relevant services with the correct details") {
          verify(mockSecureDataExchangeProxyConnector)
            .getFilesAvailableUrl(eqTo(testEori))(any())
        }
      }
    }
  }
}
