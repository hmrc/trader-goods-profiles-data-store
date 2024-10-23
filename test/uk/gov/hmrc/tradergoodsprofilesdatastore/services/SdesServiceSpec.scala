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

package uk.gov.hmrc.tradergoodsprofilesdatastore.services

import org.apache.pekko.Done
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.FileReadyUnseen
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.DownloadRecordEmailParameters
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Email
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.SdesSubmissionWorkItemRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.DateTimeFormats.convertToDateString

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class SdesServiceSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with MockitoSugar
    with OptionValues {

  private val id                            = java.util.UUID.randomUUID().toString
  private val now                           = Instant.now()
  private val clock                         = Clock.fixed(now, ZoneOffset.UTC)
  private val eori                          = "GB1234567890"
  private val retentionDays                 = "30"
  private val fileName                      = "fileName"
  private val fileSize                      = 600
  private val summary                       = DownloadDataSummary(
    id,
    eori,
    FileReadyUnseen,
    now,
    now.plus(retentionDays.toInt, ChronoUnit.DAYS),
    Some(FileInfo(fileName, fileSize, retentionDays))
  )
  private val timestamp                     = Instant.now
  private val address                       = "email@test.co.uk"
  private val email                         = Email(address, timestamp)
  private val downloadRecordEmailParameters = DownloadRecordEmailParameters(
    convertToDateString(now.plus(retentionDays.toInt, ChronoUnit.DAYS), isWelsh = false)
  )

  private val mockSdesSubmissionWorkItemRepository: SdesSubmissionWorkItemRepository =
    mock[SdesSubmissionWorkItemRepository]
  private val mockCustomsDataStoreConnector: CustomsDataStoreConnector               = mock[CustomsDataStoreConnector]
  private val mockEmailConnector: EmailConnector                                     = mock[EmailConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(
      mockSdesSubmissionWorkItemRepository,
      mockCustomsDataStoreConnector,
      mockEmailConnector
    )
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "sdes.submission.retry-after" -> "30m"
      )
      .overrides(
        bind[SdesSubmissionWorkItemRepository].toInstance(mockSdesSubmissionWorkItemRepository),
        bind[EmailConnector].toInstance(mockEmailConnector),
        bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
        bind[Clock].toInstance(clock)
      )
      .build()

  private lazy val sdesService: SdesService = app.injector.instanceOf[SdesService]

  "enqueueSubmission" - {

    "must save a work item" in {

      when(mockSdesSubmissionWorkItemRepository.pushNew(any(), any(), any())).thenReturn(Future.successful(null))

      sdesService.enqueueSubmission(summary).futureValue

      verify(mockSdesSubmissionWorkItemRepository).pushNew(summary, now)
    }

    "must fail when saving the work item fails" in {

      when(mockSdesSubmissionWorkItemRepository.pushNew(any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      sdesService.enqueueSubmission(summary).failed.futureValue
    }
  }

  "processNextSubmission" - {

    "when there is a submission in the work item queue" - {

      "must submit the work item to be emailed and return true" in {

        val workItem = WorkItem(
          id = ObjectId.get(),
          receivedAt = now.minus(1, ChronoUnit.HOURS),
          updatedAt = now.minus(1, ChronoUnit.HOURS),
          availableAt = now.minus(1, ChronoUnit.HOURS),
          status = ToDo,
          failureCount = 0,
          item = summary
        )

        when(mockSdesSubmissionWorkItemRepository.pullOutstanding(any(), any()))
          .thenReturn(Future.successful(Some(workItem)))
        when(mockCustomsDataStoreConnector.getEmail(any())(any())).thenReturn(Future.successful(Some(email)))
        when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any())).thenReturn(Future.successful(Done))
        when(mockSdesSubmissionWorkItemRepository.complete(any(), any())).thenReturn(Future.successful(true))

        sdesService.processNextSubmission().futureValue mustBe true

        verify(mockSdesSubmissionWorkItemRepository).pullOutstanding(now.minus(30, ChronoUnit.MINUTES), now)
        verify(mockCustomsDataStoreConnector).getEmail(eqTo(eori))(any())
        verify(mockEmailConnector).sendDownloadRecordEmail(eqTo(email.address), eqTo(downloadRecordEmailParameters))(
          any()
        )
        verify(mockSdesSubmissionWorkItemRepository).complete(workItem.id, ProcessingStatus.Succeeded)
      }

      "when the Email connector fails" - {

        "must mark the work item as failed and fail" in {

          val workItem = WorkItem(
            id = ObjectId.get(),
            receivedAt = now.minus(1, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS),
            availableAt = now.minus(1, ChronoUnit.HOURS),
            status = ToDo,
            failureCount = 0,
            item = summary
          )

          when(mockSdesSubmissionWorkItemRepository.pullOutstanding(any(), any()))
            .thenReturn(Future.successful(Some(workItem)))
          when(mockCustomsDataStoreConnector.getEmail(any())(any())).thenReturn(Future.successful(Some(email)))
          when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any()))
            .thenReturn(Future.failed(new RuntimeException()))
          when(mockSdesSubmissionWorkItemRepository.markAs(any(), any(), any())).thenReturn(Future.successful(true))

          sdesService.processNextSubmission().failed.futureValue

          verify(mockSdesSubmissionWorkItemRepository).pullOutstanding(now.minus(30, ChronoUnit.MINUTES), now)
          when(mockCustomsDataStoreConnector.getEmail(any())(any())).thenReturn(Future.successful(Some(email)))
          when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any())).thenReturn(Future.successful(Done))
          verify(mockSdesSubmissionWorkItemRepository).markAs(workItem.id, ProcessingStatus.Failed)
        }
      }

      "when the CustomsDataStore connector fails" - {

        "must mark the work item as failed and fail" in {

          val workItem = WorkItem(
            id = ObjectId.get(),
            receivedAt = now.minus(1, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS),
            availableAt = now.minus(1, ChronoUnit.HOURS),
            status = ToDo,
            failureCount = 0,
            item = summary
          )

          when(mockSdesSubmissionWorkItemRepository.pullOutstanding(any(), any()))
            .thenReturn(Future.successful(Some(workItem)))
          when(mockCustomsDataStoreConnector.getEmail(any())(any())).thenReturn(Future.failed(new RuntimeException()))
          when(mockSdesSubmissionWorkItemRepository.markAs(any(), any(), any())).thenReturn(Future.successful(true))

          sdesService.processNextSubmission().failed.futureValue

          verify(mockSdesSubmissionWorkItemRepository).pullOutstanding(now.minus(30, ChronoUnit.MINUTES), now)
          verify(mockCustomsDataStoreConnector).getEmail(eqTo(eori))(any())
          verify(mockSdesSubmissionWorkItemRepository).markAs(workItem.id, ProcessingStatus.Failed)
        }
      }

      "when there is no email" - {

        "must mark the work item as failed and fail" in {

          val workItem = WorkItem(
            id = ObjectId.get(),
            receivedAt = now.minus(1, ChronoUnit.HOURS),
            updatedAt = now.minus(1, ChronoUnit.HOURS),
            availableAt = now.minus(1, ChronoUnit.HOURS),
            status = ToDo,
            failureCount = 0,
            item = summary
          )

          when(mockSdesSubmissionWorkItemRepository.pullOutstanding(any(), any()))
            .thenReturn(Future.successful(Some(workItem)))
          when(mockCustomsDataStoreConnector.getEmail(any())(any())).thenReturn(Future.successful(None))
          when(mockSdesSubmissionWorkItemRepository.markAs(any(), any(), any())).thenReturn(Future.successful(true))

          sdesService.processNextSubmission().failed.futureValue

          verify(mockSdesSubmissionWorkItemRepository).pullOutstanding(now.minus(30, ChronoUnit.MINUTES), now)
          verify(mockCustomsDataStoreConnector).getEmail(eqTo(eori))(any())
          verify(mockSdesSubmissionWorkItemRepository).markAs(workItem.id, ProcessingStatus.Failed)
        }
      }

    }

    "when there is no submission in the work item queue" - {

      "must return false" in {

        when(mockSdesSubmissionWorkItemRepository.pullOutstanding(any(), any())).thenReturn(Future.successful(None))

        sdesService.processNextSubmission().futureValue mustBe false

        verify(mockSdesSubmissionWorkItemRepository).pullOutstanding(now.minus(30, ChronoUnit.MINUTES), now)
        verify(mockEmailConnector, never()).sendDownloadRecordEmail(any(), any())(any())
        verify(mockCustomsDataStoreConnector, never()).getEmail(any())(any())
        verify(mockSdesSubmissionWorkItemRepository, never()).markAs(any(), any(), any())
        verify(mockSdesSubmissionWorkItemRepository, never()).complete(any(), any())
      }
    }
  }

  "processAllSubmissions" - {

    val workItem = WorkItem(
      id = ObjectId.get(),
      receivedAt = now.minus(1, ChronoUnit.HOURS),
      updatedAt = now.minus(1, ChronoUnit.HOURS),
      availableAt = now.minus(1, ChronoUnit.HOURS),
      status = ToDo,
      failureCount = 0,
      item = summary
    )

    "must process all submissions" in {

      when(mockSdesSubmissionWorkItemRepository.pullOutstanding(any(), any())).thenReturn(
        Future.successful(Some(workItem)),
        Future.successful(Some(workItem)),
        Future.successful(Some(workItem)),
        Future.successful(None)
      )
      when(mockCustomsDataStoreConnector.getEmail(any())(any())).thenReturn(Future.successful(Some(email)))
      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any())).thenReturn(Future.successful(Done))
      when(mockSdesSubmissionWorkItemRepository.complete(any(), any())).thenReturn(Future.successful(true))

      sdesService.processAllSubmissions().futureValue

      verify(mockSdesSubmissionWorkItemRepository, times(4)).pullOutstanding(now.minus(30, ChronoUnit.MINUTES), now)
      verify(mockEmailConnector, times(3))
        .sendDownloadRecordEmail(eqTo(email.address), eqTo(downloadRecordEmailParameters))(any())
      verify(mockCustomsDataStoreConnector, times(3)).getEmail(eqTo(eori))(any())
      verify(mockSdesSubmissionWorkItemRepository, times(3)).complete(workItem.id, ProcessingStatus.Succeeded)
    }

    "must fail when one of the submissions fails" in {

      when(mockSdesSubmissionWorkItemRepository.pullOutstanding(any(), any()))
        .thenReturn(Future.successful(Some(workItem)))
      when(mockCustomsDataStoreConnector.getEmail(any())(any())).thenReturn(Future.successful(Some(email)))
      when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any()))
        .thenReturn(Future.successful(Done), Future.failed(new RuntimeException()))
      when(mockSdesSubmissionWorkItemRepository.complete(any(), any())).thenReturn(Future.successful(true))

      sdesService.processAllSubmissions().failed.futureValue

      verify(mockSdesSubmissionWorkItemRepository, times(2)).pullOutstanding(now.minus(30, ChronoUnit.MINUTES), now)
      verify(mockEmailConnector, times(2))
        .sendDownloadRecordEmail(eqTo(email.address), eqTo(downloadRecordEmailParameters))(any())
      verify(mockCustomsDataStoreConnector, times(2)).getEmail(eqTo(eori))(any())
      verify(mockSdesSubmissionWorkItemRepository, times(1)).complete(workItem.id, ProcessingStatus.Succeeded)
      verify(mockSdesSubmissionWorkItemRepository, times(1)).markAs(workItem.id, ProcessingStatus.Failed)
    }
  }
}
