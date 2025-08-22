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

package uk.gov.hmrc.tradergoodsprofilesdatastore.repositories

import org.mockito.Mockito.when
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.slf4j.MDC
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeStoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.StoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileFailedSeen, FileFailedUnseen, FileInProgress, FileReadySeen, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataSummary

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[DownloadDataSummary]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with GuiceOneAppPerSuite {

  private val testEori = "GB123456789001"
  private val id       = java.util.UUID.randomUUID().toString
  private val now      = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  val sampleDownloadDataSummary: DownloadDataSummary = DownloadDataSummary(
    summaryId = id,
    eori = testEori,
    status = FileInProgress,
    createdAt = now,
    expiresAt = now,
    fileInfo = None
  )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[StoreLatestAction].to[FakeStoreLatestAction]
    )
    .build()

  protected override val repository: DownloadDataSummaryRepository =
    app.injector.instanceOf[DownloadDataSummaryRepository]

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  ".set" - {

    "must create a downloadDataSummary when there is none" in {
      repository.set(sampleDownloadDataSummary).futureValue
      val retrievedRecord = find(byEori(testEori)).futureValue.headOption.value

      retrievedRecord mustEqual sampleDownloadDataSummary
    }

    "must update a downloadDataSummary when there is one" in {
      insert(sampleDownloadDataSummary).futureValue
      val newDownloadDataSummary: DownloadDataSummary = DownloadDataSummary(
        summaryId = java.util.UUID.randomUUID().toString,
        eori = testEori,
        status = FileInProgress,
        createdAt = now,
        expiresAt = now,
        fileInfo = None
      )

      repository.set(newDownloadDataSummary).futureValue
      val updatedRecord = find(byEori(testEori)).futureValue.headOption.value

      updatedRecord.eori mustEqual testEori
      updatedRecord.status mustBe newDownloadDataSummary.status
    }

    mustPreserveMdc(repository.set(sampleDownloadDataSummary))
  }

  ".get many" - {

    "when there is a downloadDataSummary for this eori it must get a list of downloadDataSummaries that match" in {
      insert(sampleDownloadDataSummary).futureValue
      repository.get(sampleDownloadDataSummary.eori).futureValue mustEqual Seq(sampleDownloadDataSummary)
    }

    "when there is no downloadDataSummary for this eori it must get empty List" in {
      repository.get(sampleDownloadDataSummary.eori).futureValue mustEqual Seq.empty
    }

    mustPreserveMdc(repository.get(sampleDownloadDataSummary.eori))
  }

  ".get one" - {

    "when use-x-conversation-id-header is true" - {

      "when there is a downloadDataSummary for this eori and summary id it must get it" in {

        val mockDataStoreAppConfig = mock[DataStoreAppConfig]
        when(mockDataStoreAppConfig.useXConversationIdHeader) thenReturn true

        insert(sampleDownloadDataSummary).futureValue
        insert(sampleDownloadDataSummary.copy(summaryId = java.util.UUID.randomUUID.toString)).futureValue
        repository.get(sampleDownloadDataSummary.eori, sampleDownloadDataSummary.summaryId).futureValue mustEqual Some(
          sampleDownloadDataSummary
        )
      }

      "when there is no downloadDataSummary for this eori and summaryId it must return None" in {

        val mockDataStoreAppConfig = mock[DataStoreAppConfig]
        when(mockDataStoreAppConfig.useXConversationIdHeader) thenReturn true

        repository.get(sampleDownloadDataSummary.eori, sampleDownloadDataSummary.summaryId).futureValue mustEqual None
      }
    }

    "when use-x-conversation-id-header is false" - {

      "when there are downloadDataSummaries for this eori in Progress it must return the oldest" in {

        val mockDataStoreAppConfig = mock[DataStoreAppConfig]
        when(mockDataStoreAppConfig.useXConversationIdHeader) thenReturn false

        val oldestInProgressUuid   = java.util.UUID.randomUUID.toString
        val oldestUuid             = java.util.UUID.randomUUID.toString
        val youngestInProgressUuid = java.util.UUID.randomUUID.toString

        insert(
          sampleDownloadDataSummary
            .copy(summaryId = oldestUuid, status = FileReadySeen, createdAt = Instant.now().minus(30, ChronoUnit.DAYS))
        ).futureValue
        insert(
          sampleDownloadDataSummary.copy(
            summaryId = oldestInProgressUuid,
            status = FileInProgress,
            createdAt = Instant.now().minus(20, ChronoUnit.DAYS)
          )
        ).futureValue
        insert(
          sampleDownloadDataSummary.copy(
            summaryId = youngestInProgressUuid,
            status = FileInProgress,
            createdAt = Instant.now().minus(10, ChronoUnit.DAYS)
          )
        ).futureValue
        repository
          .get(sampleDownloadDataSummary.eori, oldestInProgressUuid)
          .futureValue
          .get
          .summaryId mustEqual oldestInProgressUuid
      }

      "when there is no downloadDataSummary for this eori it must return None" in {

        val mockDataStoreAppConfig = mock[DataStoreAppConfig]
        when(mockDataStoreAppConfig.useXConversationIdHeader) thenReturn false

        repository.get(sampleDownloadDataSummary.eori, sampleDownloadDataSummary.summaryId).futureValue mustEqual None
      }
    }

    mustPreserveMdc(repository.get(sampleDownloadDataSummary.eori, sampleDownloadDataSummary.summaryId))
  }

  ".update" - {

    "must only update status from FileReadyUnseen to FileReadySeen" in {

      val downloadDataSummaryFileReadyUnseen: DownloadDataSummary = DownloadDataSummary(
        summaryId = java.util.UUID.randomUUID().toString,
        eori = testEori,
        status = FileReadyUnseen,
        createdAt = now,
        expiresAt = now,
        fileInfo = None
      )

      val downloadDataSummaryFileInProgress: DownloadDataSummary = DownloadDataSummary(
        summaryId = java.util.UUID.randomUUID().toString,
        eori = testEori,
        status = FileInProgress,
        createdAt = now,
        expiresAt = now,
        fileInfo = None
      )

      repository.set(downloadDataSummaryFileReadyUnseen).futureValue
      repository.set(downloadDataSummaryFileInProgress).futureValue

      repository.updateSeen(testEori).futureValue mustEqual 1

      val records = find(byEori(testEori)).futureValue

      val updatedRecord    = records.find(_.summaryId == downloadDataSummaryFileReadyUnseen.summaryId).value
      val notUpdatedRecord = records.find(_.summaryId == downloadDataSummaryFileInProgress.summaryId).value

      updatedRecord.status mustEqual FileReadySeen
      notUpdatedRecord.status mustEqual FileInProgress
    }

    "must only update status from FileFailedUnseen to FileFailedSeen" in {

      val downloadDataSummaryFileFailedUnseen: DownloadDataSummary = DownloadDataSummary(
        summaryId = java.util.UUID.randomUUID().toString,
        eori = testEori,
        status = FileFailedUnseen,
        createdAt = now,
        expiresAt = now,
        fileInfo = None
      )

      val downloadDataSummaryFileInProgress: DownloadDataSummary = DownloadDataSummary(
        summaryId = java.util.UUID.randomUUID().toString,
        eori = testEori,
        status = FileInProgress,
        createdAt = now,
        expiresAt = now,
        fileInfo = None
      )

      repository.set(downloadDataSummaryFileFailedUnseen).futureValue
      repository.set(downloadDataSummaryFileInProgress).futureValue

      repository.updateSeen(testEori).futureValue mustEqual 1

      val records = find(byEori(testEori)).futureValue

      val updatedRecord = records.find(_.summaryId == downloadDataSummaryFileFailedUnseen.summaryId).value
      val notUpdatedRecord = records.find(_.summaryId == downloadDataSummaryFileInProgress.summaryId).value

      updatedRecord.status mustEqual FileFailedSeen
      notUpdatedRecord.status mustEqual FileInProgress
    }

    mustPreserveMdc(repository.updateSeen("eori"))
  }

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      val ec = app.injector.instanceOf[ExecutionContext]

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }(ec).futureValue
    }
}
