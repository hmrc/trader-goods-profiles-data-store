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
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.TestConstants.testEori
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{DownloadData, DownloadDataNotification, Email, Metadata}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository

import java.time.{Clock, Instant, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val id    = java.util.UUID.randomUUID().toString
  private val now   = Instant.now
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

    "return empty list if the DownloadDataSummaries are not present" in {

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
      val retentionDaysMetaData = Metadata("RETENTION_DAYS", "30")

      val summary = DownloadDataSummary(id, testEori, FileInProgress, now, now.plus(1, ChronoUnit.SECONDS), None)

      val notification =
        DownloadDataNotification(testEori, fileName, fileSize, Seq(retentionDaysMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.getOldestInProgress(any())) thenReturn Future.successful(Some(summary))
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
      captor.getValue.summaryId mustEqual id
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

      val summary = DownloadDataSummary(id, testEori, FileInProgress, now, now, None)

      val notification =
        DownloadDataNotification(requestEori, fileName, fileSize, Seq(retentionDaysMetaData, filetypeMetaData))

      lazy val validFakePostRequest = FakeRequest("POST", submitNotificationUrl)
        .withJsonBody(Json.toJson(notification))

      val mockDownloadDataSummaryRepository = mock[DownloadDataSummaryRepository]
      val mockCustomsDataStoreConnector     = mock[CustomsDataStoreConnector]
      val mockEmailConnector                = mock[EmailConnector]

      when(mockDownloadDataSummaryRepository.set(any())) thenReturn Future.successful(Done)
      when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(None)
      when(mockDownloadDataSummaryRepository.getOldestInProgress(any())) thenReturn Future.successful(Some(summary))

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

      val summary = DownloadDataSummary(id, testEori, FileInProgress, now, now, None)

      when(mockDownloadDataSummaryRepository.getOldestInProgress(any())) thenReturn Future.successful(Some(summary))
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

          val captor: ArgumentCaptor[DownloadDataSummary] = ArgumentCaptor.forClass(classOf[DownloadDataSummary])
          verify(mockDownloadDataSummaryRepository).set(captor.capture)
          captor.getValue.eori mustEqual testEori
          captor.getValue.status mustEqual FileInProgress
        }
      }
    }
  }

  "getDownloadData" - {

    "return 200 when download data is successfully requested" in {

      val fileName = "fileName"
      val fileSize = 600

      //TODO inject a clock to check filecreated
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
