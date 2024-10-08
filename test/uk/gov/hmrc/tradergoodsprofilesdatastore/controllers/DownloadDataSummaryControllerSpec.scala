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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.TestConstants.testEori
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadySeen, FileReadyUnseen, RequestFile}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{DownloadData, DownloadDataNotification, Email, Metadata}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "getDownloadDataSummary" - {

    "return 200 and the DownloadDataSummary if it exists" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .getDownloadDataSummary(testEori)
        .url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataSummaryUrl)
      val downloadDataSummary      = DownloadDataSummary(testEori, FileInProgress, None)

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

      verify(mockDownloadDataSummaryRepository).get(testEori)
    }

    "return 404 if the DownloadDataSummary is not present" in {

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .getDownloadDataSummary(testEori)
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
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", "30")

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(Some(email))
      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any())) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
          bind[EmailConnector].toInstance(mockEmailConnector)
        )
        .build()
      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.NO_CONTENT
      }

      val captor: ArgumentCaptor[DownloadDataSummary] = ArgumentCaptor.forClass(classOf[DownloadDataSummary])
      verify(mockDownloadDataSummaryRepository).set(captor.capture)
      captor.getValue.eori mustEqual testEori
      captor.getValue.status mustEqual FileReadyUnseen
      captor.getValue.fileInfo.get.fileName mustEqual fileName
      captor.getValue.fileInfo.get.fileSize mustEqual fileSize
      captor.getValue.fileInfo.get.retentionDays mustEqual retentionDaysMetaData.value
    }

    "return 404 if CustomsDataStoreConnector return None" in {

      val requestEori                = "GB123456789099"
      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", "30")
      val filetypeMetaData      = Metadata("FILETYPE", "csv")

      val notification =
        DownloadDataNotification(requestEori, fileName, fileSize, Seq(retentionDaysMetaData, filetypeMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
          bind[EmailConnector].toInstance(mockEmailConnector)
        )
        .build()
      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }

      val captor: ArgumentCaptor[DownloadDataSummary] = ArgumentCaptor.forClass(classOf[DownloadDataSummary])
      verify(mockDownloadDataSummaryRepository).set(captor.capture)
      captor.getValue.eori mustEqual requestEori
      captor.getValue.status mustEqual FileReadyUnseen
      captor.getValue.fileInfo.get.fileName mustEqual fileName
      captor.getValue.fileInfo.get.fileSize mustEqual fileSize
      captor.getValue.fileInfo.get.retentionDays mustEqual retentionDaysMetaData.value

      withClue("Should not call Notification Connector") {
        verify(mockEmailConnector, never()).sendDownloadRecordEmail(any(), any())(any())
      }
    }

    "return error if EmailConnector fails" in {

      lazy val submitNotificationUrl = routes.DownloadDataSummaryController
        .submitNotification()
        .url

      val fileName              = "fileName"
      val fileSize              = 600
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", "30")
      val filetypeMetaData      = Metadata("FILETYPE", "csv")

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData, filetypeMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(Some(email))
      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any())) thenReturn Future.failed(
        new RuntimeException("Failed to send the email")
      )

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository),
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
          bind[EmailConnector].toInstance(mockEmailConnector)
        )
        .build()
      running(application) {
        intercept[RuntimeException] {
          await(route(application, validFakePostRequest).value)
        }
      }

      val captor: ArgumentCaptor[DownloadDataSummary] = ArgumentCaptor.forClass(classOf[DownloadDataSummary])
      verify(mockDownloadDataSummaryRepository).set(captor.capture)
      captor.getValue.eori mustEqual testEori
      captor.getValue.status mustEqual FileReadyUnseen
      captor.getValue.fileInfo.get.fileName mustEqual fileName
      captor.getValue.fileInfo.get.fileSize mustEqual fileSize
      captor.getValue.fileInfo.get.retentionDays mustEqual retentionDaysMetaData.value
    }
  }

  "requestDownloadData" - {

    "return 202 when data is successfully requested" in {
      val downloadDataSummary = DownloadDataSummary(testEori, FileInProgress, None)

      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .requestDownloadData(testEori)
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
            .getRequestDownloadData(eqTo(testEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .set(eqTo(downloadDataSummary))
        }
      }
    }
  }

  "getDownloadData" - {

    "return 200 when download data is successfully requested" in {

      val fileName      = "fileName"
      val fileSize      = 600
      val fileCreated   = Instant.now.minus(20, ChronoUnit.DAYS)
      val retentionDays = "30"

      val downloadDataSummaryUnseen = DownloadDataSummary(
        testEori,
        FileReadyUnseen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays))
      )

      val downloadDataSummarySeen = DownloadDataSummary(
        testEori,
        FileReadySeen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays))
      )

      val url                      = "/some-url"
      val fileRoleMetadata         = Metadata("FileRole", "C79Certificate")
      val periodStartYearMetadata  = Metadata("PeriodStartYear", "2020")
      val retentionDaysMetadata    = Metadata("RETENTION_DAYS", retentionDays)
      val periodStartMonthMetadata = Metadata("PeriodStartMonth", "08")

      val downloadData             = DownloadData(
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
      lazy val downloadDataUrl     = routes.DownloadDataSummaryController
        .getDownloadData(testEori)
        .url
      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataUrl)

      val mockSecureDataExchangeProxyConnector = mock[SecureDataExchangeProxyConnector]
      when(
        mockSecureDataExchangeProxyConnector.getFilesAvailableUrl(any())(any())
      ) thenReturn Future.successful(Seq(downloadData))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.get(any())
      ) thenReturn Future.successful(Some(downloadDataSummaryUnseen))

      when(
        mockDownloadDataSummaryRepository.set(any())
      ) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          bind[SecureDataExchangeProxyConnector].toInstance(mockSecureDataExchangeProxyConnector),
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()

      running(application) {
        val request = validFakeGetRequest
          .withHeaders("Content-Type" -> "application/json")
        val result  = route(application, request).value
        status(result) shouldBe OK
        contentAsJson(result) mustEqual Json.toJson(downloadData)

        withClue("must call the relevant services with the correct details") {
          verify(mockSecureDataExchangeProxyConnector)
            .getFilesAvailableUrl(eqTo(testEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .get(eqTo(testEori))
          verify(mockDownloadDataSummaryRepository)
            .set(eqTo(downloadDataSummarySeen))
        }
      }
    }

    "return 404 when download data is not in list" in {

      val fileName      = "fileName"
      val fileSize      = 600
      val fileCreated   = Instant.now.minus(20, ChronoUnit.DAYS)
      val retentionDays = "30"

      val downloadDataSummary = DownloadDataSummary(
        testEori,
        FileReadySeen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays))
      )

      lazy val downloadDataUrl = routes.DownloadDataSummaryController
        .getDownloadData(testEori)
        .url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataUrl)

      val mockSecureDataExchangeProxyConnector = mock[SecureDataExchangeProxyConnector]
      when(
        mockSecureDataExchangeProxyConnector.getFilesAvailableUrl(any())(any())
      ) thenReturn Future.successful(Seq.empty)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.get(any())
      ) thenReturn Future.successful(Some(downloadDataSummary))

      val application = applicationBuilder()
        .overrides(
          bind[SecureDataExchangeProxyConnector].toInstance(mockSecureDataExchangeProxyConnector),
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()

      running(application) {
        val request = validFakeGetRequest
          .withHeaders("Content-Type" -> "application/json")
        val result  = route(application, request).value
        status(result) shouldBe NOT_FOUND

        withClue("must call the relevant services with the correct details") {
          verify(mockSecureDataExchangeProxyConnector)
            .getFilesAvailableUrl(eqTo(testEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .get(eqTo(testEori))
        }
      }
    }

    "return 404 when fileInfo is not in summary" in {

      val downloadDataSummary = DownloadDataSummary(
        testEori,
        FileReadySeen,
        None
      )

      lazy val downloadDataUrl = routes.DownloadDataSummaryController
        .getDownloadData(testEori)
        .url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataUrl)

      val mockSecureDataExchangeProxyConnector = mock[SecureDataExchangeProxyConnector]
      when(
        mockSecureDataExchangeProxyConnector.getFilesAvailableUrl(any())(any())
      ) thenReturn Future.successful(Seq.empty)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.get(any())
      ) thenReturn Future.successful(Some(downloadDataSummary))

      val application = applicationBuilder()
        .overrides(
          bind[SecureDataExchangeProxyConnector].toInstance(mockSecureDataExchangeProxyConnector),
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()

      running(application) {
        val request = validFakeGetRequest
          .withHeaders("Content-Type" -> "application/json")
        val result  = route(application, request).value
        status(result) shouldBe NOT_FOUND

        withClue("must call the relevant services with the correct details") {
          verify(mockSecureDataExchangeProxyConnector, never())
            .getFilesAvailableUrl(eqTo(testEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .get(eqTo(testEori))
        }
      }
    }

    "return 404 when summary is not the correct status" in {

      val downloadDataSummary = DownloadDataSummary(
        testEori,
        RequestFile,
        None
      )

      lazy val downloadDataUrl = routes.DownloadDataSummaryController
        .getDownloadData(testEori)
        .url

      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataUrl)

      val mockSecureDataExchangeProxyConnector = mock[SecureDataExchangeProxyConnector]
      when(
        mockSecureDataExchangeProxyConnector.getFilesAvailableUrl(any())(any())
      ) thenReturn Future.successful(Seq.empty)

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(
        mockDownloadDataSummaryRepository.get(any())
      ) thenReturn Future.successful(Some(downloadDataSummary))

      val application = applicationBuilder()
        .overrides(
          bind[SecureDataExchangeProxyConnector].toInstance(mockSecureDataExchangeProxyConnector),
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()

      running(application) {
        val request = validFakeGetRequest
          .withHeaders("Content-Type" -> "application/json")
        val result  = route(application, request).value
        status(result) shouldBe NOT_FOUND

        withClue("must call the relevant services with the correct details") {
          verify(mockSecureDataExchangeProxyConnector, never())
            .getFilesAvailableUrl(eqTo(testEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .get(eqTo(testEori))
        }
      }
    }

  }
}
