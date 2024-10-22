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
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.StoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.FileInProgress
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

  ".get" - {

    "when there is a downloadDataSummary for this eori it must get a list of downloadDataSummaries that match" in {
      insert(sampleDownloadDataSummary).futureValue
      repository.get(sampleDownloadDataSummary.eori).futureValue mustEqual Seq(sampleDownloadDataSummary)
    }

    "when there is no downloadDataSummary for this eori it must get empty List" in {
      repository.get(sampleDownloadDataSummary.eori).futureValue mustEqual Seq.empty
    }

    mustPreserveMdc(repository.get(sampleDownloadDataSummary.eori))
  }

  ".getOldestInProgress" - {

    "must get the latest in progress summary that matches the eori" in {
      insert(sampleDownloadDataSummary).futureValue
      repository
        .getOldestInProgress(sampleDownloadDataSummary.eori)
        .futureValue
        .value mustEqual sampleDownloadDataSummary
    }

    "when there is no downloadDataSummary for this eori it must return None" in {
      repository.getOldestInProgress(sampleDownloadDataSummary.eori).futureValue mustEqual None
    }

    mustPreserveMdc(repository.get(sampleDownloadDataSummary.eori))
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
