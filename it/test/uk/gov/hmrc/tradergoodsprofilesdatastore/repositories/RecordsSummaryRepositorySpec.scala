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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class RecordsSummaryRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[RecordsSummary]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with GuiceOneAppPerSuite {

  val testEori = "GB123456789001"
  val now      = Instant.now().truncatedTo(ChronoUnit.MILLIS)

  val sampleRecordsSummary: RecordsSummary = RecordsSummary(
    eori = testEori,
    currentUpdate = Some(Update(0, 0)),
    lastUpdated = now
  )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent)
    )
    .build()

  protected override val repository: RecordsSummaryRepository =
    app.injector.instanceOf[RecordsSummaryRepository]

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  private def byLastUpdated(lastUpdated: Instant): Bson = Filters.lt("lastUpdated", lastUpdated)

  ".set" - {

    "must create a recordsSummary when there is none" in {
      val eori          = "EORI"
      repository.set(eori, Some(Update(0, 0)), now).futureValue
      val updatedRecord = find(byEori(eori)).futureValue.headOption.value

      updatedRecord.eori mustEqual eori
      updatedRecord.currentUpdate.value mustEqual Update(0, 0)
      updatedRecord.lastUpdated mustEqual now
    }

    "must update a recordsSummary when there is one" in {
      insert(sampleRecordsSummary).futureValue

      repository.set(testEori, None, now).futureValue
      val updatedRecord = find(byEori(testEori)).futureValue.headOption.value

      updatedRecord.eori mustEqual testEori
      updatedRecord.currentUpdate mustBe None
      updatedRecord.lastUpdated mustEqual now
    }

    mustPreserveMdc(repository.set(testEori, None, now))
  }

  ".update" - {

    "must update `currentUpdate` when it is given" in {
      val eori          = "EORI"
      repository.set(eori, Some(Update(0, 0)), now).futureValue
      repository.update(eori, Some(Update(1, 1)), None).futureValue
      val updatedRecord = find(byEori(eori)).futureValue.headOption.value

      updatedRecord.eori mustEqual eori
      updatedRecord.currentUpdate.value mustEqual Update(1, 1)
    }

    "must remove `currentUpdate` when it is not given" in {
      val eori          = "EORI"
      repository.set(eori, Some(Update(0, 0)), now).futureValue
      repository.update(eori, None, None).futureValue
      val updatedRecord = find(byEori(eori)).futureValue.headOption.value

      updatedRecord.eori mustEqual eori
      updatedRecord.currentUpdate mustBe None
    }

    "must update `lastUpdated` when it is given" in {
      val eori          = "EORI"
      val later         = now.plus(1, ChronoUnit.DAYS)
      repository.set(eori, Some(Update(0, 0)), now).futureValue
      repository.update(eori, None, Some(later)).futureValue
      val updatedRecord = find(byEori(eori)).futureValue.headOption.value

      updatedRecord.eori mustEqual eori
      updatedRecord.lastUpdated mustEqual later
    }

    "must not update `lastUpdated` when it is not given" in {
      val eori          = "EORI"
      repository.set(eori, Some(Update(0, 0)), now).futureValue
      repository.update(eori, None, None).futureValue
      val updatedRecord = find(byEori(eori)).futureValue.headOption.value

      updatedRecord.eori mustEqual eori
      updatedRecord.lastUpdated mustEqual now
    }

    "must fail when an existing record is not found" in {
      val eori = "EORI"
      repository.update(eori, None, None).failed.futureValue
    }

    mustPreserveMdc(repository.update("eori", None, None).failed)
  }

  ".get" - {

    "when there is a recordsSummary for this eori it must get the recordsSummary" in {
      insert(sampleRecordsSummary).futureValue
      val result = repository.get(sampleRecordsSummary.eori).futureValue
      result.value mustEqual sampleRecordsSummary
    }

    "when there is no recordsSummary for this eori it must return None" in {
      repository.get(sampleRecordsSummary.eori).futureValue must not be defined
    }

    "return records when lastUpdated date is before the given date" in {
      val lastUpdated                          = now.minus(180, ChronoUnit.DAYS)
      val sampleRecordsSummary: RecordsSummary = RecordsSummary(
        eori = testEori,
        currentUpdate = Some(Update(0, 0)),
        lastUpdated = now.minus(182, ChronoUnit.DAYS)
      )

      insert(sampleRecordsSummary).futureValue

      val result = repository.getByLastUpdatedBefore(lastUpdated)

      result.futureValue.size mustBe 1
    }

    "return none when lastUpdated date is after the given date" in {
      val lastUpdated                          = now.minus(180, ChronoUnit.DAYS)
      val sampleRecordsSummary: RecordsSummary = RecordsSummary(
        eori = testEori,
        currentUpdate = Some(Update(0, 0)),
        lastUpdated = now.minus(80, ChronoUnit.DAYS)
      )

      insert(sampleRecordsSummary).futureValue

      val result = repository.getByLastUpdatedBefore(lastUpdated)

      result.futureValue.size mustBe 0
    }

    "return none when lastUpdated date is equal to the given date" in {
      val lastUpdated                          = now.minus(180, ChronoUnit.DAYS)
      val sampleRecordsSummary: RecordsSummary = RecordsSummary(
        eori = testEori,
        currentUpdate = Some(Update(0, 0)),
        lastUpdated = now.minus(180, ChronoUnit.DAYS)
      )

      insert(sampleRecordsSummary).futureValue

      val result = repository.getByLastUpdatedBefore(lastUpdated)

      result.futureValue.size mustBe 0
    }

    mustPreserveMdc(repository.get(sampleRecordsSummary.eori))
  }

  ".delete" - {
    "when there is a recordsSummary for this eori" in {
      insert(sampleRecordsSummary).futureValue

      val result = repository.deleteByEori(sampleRecordsSummary.eori).futureValue

      result mustEqual 1

    }

    "when there is a no recordsSummary for this eori it must return 0" in {
      val result = repository.deleteByEori(sampleRecordsSummary.eori).futureValue

      result mustEqual 0
    }

    mustPreserveMdc(repository.deleteByEori(sampleRecordsSummary.eori))
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
