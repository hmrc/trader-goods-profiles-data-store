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

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.when
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class RecordsSummaryRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with PlayMongoRepositorySupport[RecordsSummary]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareDatabase()
  }

  val testEori = "GB123456789001"

  val sampleRecordsSummary: RecordsSummary = RecordsSummary(
    eori = testEori,
    recordsUpdating = false,
    lastUpdated = Instant.now,
    recordsStored = 0,
    recordsToStore = 0
  )

  protected override val repository = new RecordsSummaryRepository(mongoComponent = mongoComponent)

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  ".set" - {

    "must create a recordsSummary when there is none" in {
      val setResult     = repository.set(testEori, recordsUpdating = false, 0, 0).futureValue
      val updatedRecord = find(byEori(testEori)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord.eori mustEqual testEori
    }

    "must update a recordsSummary when there is one" in {
      insert(sampleRecordsSummary).futureValue

      val setResult     = repository.set(testEori, recordsUpdating = true, 0, 0).futureValue
      val updatedRecord = find(byEori(testEori)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord.eori mustEqual testEori
      updatedRecord.recordsUpdating mustEqual true
    }
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

  }

}
