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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Assessment, Condition, GoodsItemRecords}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class RecordsRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with PlayMongoRepositorySupport[GoodsItemRecords]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareDatabase()
  }
  val sampleCondition: Condition = Condition(
    `type` = Some("abc123"),
    conditionId = Some("Y923"),
    conditionDescription =
      Some("Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"),
    conditionTraderText = Some("Excluded product")
  )

  val sampleAssessment: Assessment             = Assessment(
    assessmentId = Some("abc123"),
    primaryCategory = Some(1),
    condition = Some(sampleCondition)
  )
  val sampleGoodsItemRecords: GoodsItemRecords = GoodsItemRecords(
    eori = "GB123456789001",
    actorId = "GB098765432112",
    recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    traderRef = "BAN001001",
    comcode = "10410100",
    adviceStatus = "Not requested",
    goodsDescription = "Organic bananas",
    countryOfOrigin = "EC",
    category = 3,
    assessments = Some(Seq(sampleAssessment)),
    supplementaryUnit = Some(500),
    measurementUnit = Some("square meters(m^2)"),
    comcodeEffectiveFromDate = Instant.parse("2024-10-12T16:12:34Z"),
    comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
    version = 1,
    active = true,
    toReview = false,
    reviewReason = Some("no reason"),
    declarable = "IMMI ready",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = "RMS-GB-123456",
    niphlNumber = "6 S12345",
    locked = false,
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  protected override val repository = new RecordsRepository(mongoComponent = mongoComponent)

  private def byRecordId(recordId: String): Bson = Filters.equal("recordId", sampleGoodsItemRecords.recordId)

  ".set" - {

    "must create a record when there is none" in {
      val setResult     = repository.saveRecords(Seq(sampleGoodsItemRecords)).futureValue
      val updatedRecord = find(byRecordId(sampleGoodsItemRecords.recordId)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual sampleGoodsItemRecords
    }

    "must update a record when there is one" in {
      repository.saveRecords(Seq(sampleGoodsItemRecords)).futureValue
      val modifiedGoodsItemRecords = sampleGoodsItemRecords.copy(ukimsNumber = "new-ukims")
      val expectedGoodsItemRecords = sampleGoodsItemRecords.copy(ukimsNumber = "new-ukims")

      val saveResult    = repository.saveRecords(Seq(modifiedGoodsItemRecords)).futureValue
      val updatedRecord = find(byRecordId(sampleGoodsItemRecords.recordId)).futureValue.headOption.value

      saveResult mustEqual true
      updatedRecord mustEqual expectedGoodsItemRecords
    }
  }

}
