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
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadySeen, FileReadyUnseen, RequestFile}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{DownloadData, DownloadDataNotification, Metadata}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository

import java.time.Instant
import java.time.temporal.ChronoUnit
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
      val downloadDataSummary      = DownloadDataSummary(requestEori, FileInProgress, None)

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

      verify(mockDownloadDataSummaryRepository).get(requestEori)
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

      verify(mockDownloadDataSummaryRepository).get(requestEori)
    }
  }

  "submitDownloadDataSummary" - {

    "return 200 and the DownloadDataSummary if it exists" in {

      val requestEori                 = "GB123456789099"
      lazy val downloadDataSummaryUrl = routes.DownloadDataSummaryController
        .submitDownloadDataSummary(requestEori)
        .url
      val downloadDataSummary         = DownloadDataSummary(requestEori, FileReadySeen, None)

      lazy val validFakePostRequest =
        FakeRequest("POST", downloadDataSummaryUrl).withJsonBody(Json.toJson(downloadDataSummary))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(
        Done
      )

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.NO_CONTENT
      }

      verify(mockDownloadDataSummaryRepository).set(eqTo(downloadDataSummary))
    }
  }

  "submitNotification" - {

    "return 204 if the notification is submitted successfully" in {

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
      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(
        Done
      )

      val application = applicationBuilder()
        .overrides(
          bind[DownloadDataSummaryRepository].toInstance(mockDownloadDataSummaryRepository)
        )
        .build()
      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.NO_CONTENT
      }

      val captor: ArgumentCaptor[DownloadDataSummary] = ArgumentCaptor.forClass(classOf[DownloadDataSummary])
      verify(mockDownloadDataSummaryRepository).set(captor.capture)
      captor.getValue.eori mustEqual requestEori
      captor.getValue.status mustEqual FileReadyUnseen
      captor.getValue.fileInfo.get.fileType mustEqual filetypeMetaData.value
      captor.getValue.fileInfo.get.fileName mustEqual fileName
      captor.getValue.fileInfo.get.fileSize mustEqual fileSize
      captor.getValue.fileInfo.get.retentionDays mustEqual retentionDaysMetaData.value
    }
  }

  "requestDownloadData" - {

    "return 202 when data is successfully requested" in {
      val requestEori         = "GB123456789099"
      val downloadDataSummary = DownloadDataSummary(requestEori, FileInProgress, None)

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

  "getDownloadData" - {

    "return 200 when download data is successfully requested" in {

      val requestEori   = "GB123456789099"
      val fileName      = "fileName"
      val fileSize      = 600
      val fileCreated   = Instant.now.minus(20, ChronoUnit.DAYS)
      val retentionDays = "30"
      val fileType      = "CSV"

      val downloadDataSummary = DownloadDataSummary(
        requestEori,
        FileReadySeen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays, fileType))
      )

      val url                       = "/some-url"
      val fileRoleMetadata          = Metadata("FileRole", "C79Certificate")
      val retentionFileTypeMetadata = Metadata("RETENTION_FILE_TYPE", "TraderStatement")
      val periodStartYearMetadata   = Metadata("PeriodStartYear", "2020")
      val fileTypeMetadata          = Metadata("FileType", fileType)
      val retentionDaysMetadata     = Metadata("RETENTION_DAYS", retentionDays)
      val periodStartMonthMetadata  = Metadata("PeriodStartMonth", "08")

      val downloadData             = DownloadData(
        url,
        fileName,
        fileSize,
        Seq(
          fileRoleMetadata,
          retentionFileTypeMetadata,
          periodStartYearMetadata,
          fileTypeMetadata,
          retentionDaysMetadata,
          periodStartMonthMetadata
        )
      )
      lazy val downloadDataUrl     = routes.DownloadDataSummaryController
        .getDownloadData(requestEori)
        .url
      lazy val validFakeGetRequest = FakeRequest("GET", downloadDataUrl)

      val mockSecureDataExchangeProxyConnector = mock[SecureDataExchangeProxyConnector]
      when(
        mockSecureDataExchangeProxyConnector.getFilesAvailableUrl(any())(any())
      ) thenReturn Future.successful(Seq(downloadData))

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
        status(result) shouldBe OK
        contentAsJson(result) mustEqual Json.toJson(downloadData)

        withClue("must call the relevant services with the correct details") {
          verify(mockSecureDataExchangeProxyConnector)
            .getFilesAvailableUrl(eqTo(requestEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .get(eqTo(requestEori))
        }
      }
    }

    "return 404 when download data is not in list" in {

      val requestEori   = "GB123456789099"
      val fileName      = "fileName"
      val fileSize      = 600
      val fileCreated   = Instant.now.minus(20, ChronoUnit.DAYS)
      val retentionDays = "30"
      val fileType      = "CSV"

      val downloadDataSummary = DownloadDataSummary(
        requestEori,
        FileReadySeen,
        Some(FileInfo(fileName, fileSize, fileCreated, retentionDays, fileType))
      )

      lazy val downloadDataUrl = routes.DownloadDataSummaryController
        .getDownloadData(requestEori)
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
            .getFilesAvailableUrl(eqTo(requestEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .get(eqTo(requestEori))
        }
      }
    }

    "return 404 when fileInfo is not in summary" in {

      val requestEori = "GB123456789099"

      val downloadDataSummary = DownloadDataSummary(
        requestEori,
        FileReadySeen,
        None
      )

      lazy val downloadDataUrl = routes.DownloadDataSummaryController
        .getDownloadData(requestEori)
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
            .getFilesAvailableUrl(eqTo(requestEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .get(eqTo(requestEori))
        }
      }
    }

    "return 404 when summary is not the correct status" in {

      val requestEori = "GB123456789099"

      val downloadDataSummary = DownloadDataSummary(
        requestEori,
        RequestFile,
        None
      )

      lazy val downloadDataUrl = routes.DownloadDataSummaryController
        .getDownloadData(requestEori)
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
            .getFilesAvailableUrl(eqTo(requestEori))(any())
          verify(mockDownloadDataSummaryRepository)
            .get(eqTo(requestEori))
        }
      }
    }

  }
}
