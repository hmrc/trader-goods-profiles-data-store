/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.FileInProgress
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.DownloadRecordFailureEmailParameters
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Email
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class DownloadFailureServiceSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with MockitoSugar
    with OptionValues {

  // we’ll still keep an implicit hc in scope; matchers version additionally matches it explicitly
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val now   = Instant.now()
  private val clock = Clock.fixed(now, ZoneOffset.UTC)
  private val eori  = "GB1234567890"

  private def mkSummary(
    e: String = eori,
    createdAt: Instant = now.minus(25, ChronoUnit.HOURS),
    status: DownloadDataStatus = FileInProgress
  ): DownloadDataSummary =
    DownloadDataSummary(
      summaryId = java.util.UUID.randomUUID().toString,
      eori = e,
      status = status,
      createdAt = createdAt,
      expiresAt = now.plus(30, ChronoUnit.DAYS),
      fileInfo = Some(FileInfo("file.csv", 123, "30"))
    )

  private val email = Email("test@example.com", now)

  private val mockRepo    = mock[DownloadDataSummaryRepository]
  private val mockCustoms = mock[CustomsDataStoreConnector]
  private val mockEmail   = mock[EmailConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepo, mockCustoms, mockEmail)
  }

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DownloadDataSummaryRepository].toInstance(mockRepo),
        bind[CustomsDataStoreConnector].toInstance(mockCustoms),
        bind[EmailConnector].toInstance(mockEmail),
        bind[Clock].toInstance(clock)
      )
      .build()

  private lazy val service: DownloadFailureService =
    app.injector.instanceOf[DownloadFailureService]

  "DownloadFailureService.processStaleDownloads" - {

    "do nothing if no stale downloads are found" in {
      when(mockRepo.findStaleSummaries(any()))
        .thenReturn(Future.successful(Seq.empty))

      val result = service.processStaleDownloads().futureValue

      result mustBe Done
      verifyNoInteractions(mockCustoms, mockEmail)
      verify(mockRepo, never()).markAsFailed(any())
    }

    "mark stale downloads as failed and send email" in {
      val s = mkSummary()

      when(mockRepo.findStaleSummaries(any()))
        .thenReturn(Future.successful(Seq(s)))
      when(mockRepo.markAsFailed(eqTo(eori)))
        .thenReturn(Future.successful(1L))
      when(mockCustoms.getEmail(eqTo(eori))(any()))
        .thenReturn(Future.successful(Some(email)))
      when(
        mockEmail.sendDownloadRecordFailureEmail(eqTo(email.address), eqTo(DownloadRecordFailureEmailParameters()))(
          any()
        )
      )
        .thenReturn(Future.successful(Done))

      val result = service.processStaleDownloads().futureValue

      result mustBe Done
      verify(mockRepo).markAsFailed(eqTo(eori))
      verify(mockCustoms).getEmail(eqTo(eori))(any())
      verify(mockEmail)
        .sendDownloadRecordFailureEmail(eqTo(email.address), eqTo(DownloadRecordFailureEmailParameters()))(any())
    }

    "skip email if markAsFailed updates nothing" in {
      val s = mkSummary()

      when(mockRepo.findStaleSummaries(any()))
        .thenReturn(Future.successful(Seq(s)))
      when(mockRepo.markAsFailed(eqTo(eori)))
        .thenReturn(Future.successful(0L))

      val result = service.processStaleDownloads().futureValue

      result mustBe Done
      verify(mockRepo).markAsFailed(eqTo(eori))
      verifyNoInteractions(mockCustoms, mockEmail)
    }

    "recover gracefully if email cannot be found" in {
      val s = mkSummary()

      when(mockRepo.findStaleSummaries(any()))
        .thenReturn(Future.successful(Seq(s)))
      when(mockRepo.markAsFailed(eqTo(eori)))
        .thenReturn(Future.successful(1L))
      when(mockCustoms.getEmail(eqTo(eori))(any()))
        .thenReturn(Future.successful(None))

      val result = service.processStaleDownloads().futureValue

      result mustBe Done
      verify(mockCustoms).getEmail(eqTo(eori))(any())
      verify(mockEmail, never()).sendDownloadRecordFailureEmail(any(), any())(any())
    }

    "recover gracefully if sending email fails" in {
      val s = mkSummary()

      when(mockRepo.findStaleSummaries(any()))
        .thenReturn(Future.successful(Seq(s)))
      when(mockRepo.markAsFailed(eqTo(eori)))
        .thenReturn(Future.successful(1L))
      when(mockCustoms.getEmail(eqTo(eori))(any()))
        .thenReturn(Future.successful(Some(email)))
      when(
        mockEmail.sendDownloadRecordFailureEmail(eqTo(email.address), eqTo(DownloadRecordFailureEmailParameters()))(
          any()
        )
      )
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result = service.processStaleDownloads().futureValue

      result mustBe Done
      verify(mockCustoms).getEmail(eqTo(eori))(any())
      verify(mockEmail)
        .sendDownloadRecordFailureEmail(eqTo(email.address), eqTo(DownloadRecordFailureEmailParameters()))(any())
    }
  }
}
