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

package uk.gov.hmrc.tradergoodsprofilesdatastore.worker

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.verify
import org.mockito.MockitoSugar.when
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.TestConstants.testEori
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GoodsItemRecord
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class ClearCacheWorkerIntegrationSpec
    extends PlaySpec
    with DefaultPlayMongoRepositorySupport[RecordsSummary]
    with CleanMongoCollectionSupport
    with IntegrationPatience
    with Eventually {

  implicit val ec: ExecutionContext                                = ExecutionContext.global
  protected override lazy val repository: RecordsSummaryRepository = new RecordsSummaryRepository(mongoComponent)

  "should start ClearCacheWorker" in {
    lazy val recordsRepository = new RecordsRepository(mongoComponent)
    recordsRepository.updateRecords(testEori, Seq(goodsItemRecord)).futureValue
    insert(sampleRecordSummary).futureValue

    val app: Application = buildApplication(repository, recordsRepository)

    running(app) {
      eventually {
        val recordSummaryresult = find(Filters.eq("eori", testEori)).futureValue
        recordSummaryresult.size mustBe 0
      }
      eventually {
        val recordsResult = recordsRepository.getMany(testEori, None, None).futureValue
        recordsResult.size mustBe 0
      }
    }
  }

  "should fail to clear the cache in case of error" in {
    val mockRecordsSummaryRepository = mock[RecordsSummaryRepository]
    val mockRecordsRepository        = mock[RecordsRepository]

    when(mockRecordsSummaryRepository.getByLastUpdatedBefore(any))
      .thenReturn(
        Future.failed(new RuntimeException("error")),
        Future.successful(Seq(sampleRecordSummary)),
        Future.successful(Seq.empty)
      )
    when(mockRecordsSummaryRepository.deleteByEori(any)).thenReturn(Future.successful(1))
    when(mockRecordsRepository.deleteRecordsByEori(any)).thenReturn(Future.successful(1))

    val app: Application = buildApplication(mockRecordsSummaryRepository, mockRecordsRepository)

    running(app) {

      eventually {
        verify(mockRecordsSummaryRepository).deleteByEori(eqTo(testEori))
        verify(mockRecordsRepository).deleteRecordsByEori(eqTo(testEori))
        successful()
      }
    }
  }

  private def buildApplication(
    mockRecordsSummaryRepository: RecordsSummaryRepository,
    mockRecordsRepository: RecordsRepository
  ): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent),
        bind[RecordsSummaryRepository].toInstance(mockRecordsSummaryRepository),
        bind[RecordsRepository].toInstance(mockRecordsRepository)
      )
      .configure(
        Map(
          "workers.clear-cache-worker.initial-delay" -> "1 milliseconds",
          "workers.clear-cache-worker.interval"      -> "1 milliseconds"
        )
      )
      .build()

  private def goodsItemRecord =
    GoodsItemRecord(
      eori = testEori,
      actorId = "GB098765432112",
      recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      traderRef = "BAN001001",
      comcode = "10410100",
      adviceStatus = "Not requested",
      goodsDescription = "Organic bananas",
      countryOfOrigin = "EC",
      category = Some(3),
      assessments = None,
      supplementaryUnit = Some(500),
      measurementUnit = Some("square meters(m^2)"),
      comcodeEffectiveFromDate = Instant.parse("2024-10-12T16:12:34Z"),
      comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
      version = 1,
      active = true,
      toReview = false,
      reviewReason = Some("no reason"),
      declarable = "IMMI Ready",
      ukimsNumber = Some("XIUKIM47699357400020231115081800"),
      nirmsNumber = Some("RMS-GB-123456"),
      niphlNumber = Some("6 S12345"),
      createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
      updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
    )

  private def sampleRecordSummary =
    RecordsSummary(
      eori = testEori,
      currentUpdate = Some(Update(0, 0)),
      lastUpdated = Instant.now().minus(182, ChronoUnit.DAYS)
    )
}
