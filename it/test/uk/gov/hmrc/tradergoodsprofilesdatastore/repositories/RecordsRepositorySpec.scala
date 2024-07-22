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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Assessment, Condition, GoodsItemRecord}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class RecordsRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with PlayMongoRepositorySupport[GoodsItemRecord]
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

  val sampleAssessment: Assessment = Assessment(
    assessmentId = Some("abc123"),
    primaryCategory = Some(1),
    condition = Some(sampleCondition)
  )

  private val testrecordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  val sampleUpdateRequest: UpdateRecordRequest = UpdateRecordRequest(
    actorId = "GB098765432112",
    traderRef = Some("updated-reference")
  )

  val testEori                                = "GB123456789001"
  val sampleGoodsItemRecords: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = testrecordId,
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
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  val sampleGoodsItemRecords2: GoodsItemRecord = GoodsItemRecord(
    eori = "GB123456789001",
    actorId = "GB098765432112",
    recordId = testrecordId,
    traderRef = "BAN001002",
    comcode = "20410101",
    adviceStatus = "Not requested",
    goodsDescription = "Apples",
    countryOfOrigin = "AU",
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
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  private val traderRefSearchTerm               = "BAN001002"
  private val traderRefPartialSearchTerm        = "1001"
  private val traderRefSearchField              = "traderRef"
  private val goodsDescriptionSearchTerm        = "Tomatoes"
  private val goodsDescriptionPartialSearchTerm = "Organic"
  private val goodsDescriptionSearchField       = "goodsDescription"
  private val comCodeSearchTerm                 = "10410100"
  private val comCodePartialSearchTerm          = "10410"
  private val comCodeSearchField                = "comcode"

  private val latestRecordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204p"

  val latestGoodsItemRecords: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = latestRecordId,
    traderRef = "BAN001003",
    comcode = "30410102",
    adviceStatus = "Not requested",
    goodsDescription = "Tomatoes",
    countryOfOrigin = "AU",
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
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-11-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-11-12T16:12:34Z")
  )

  private val inactiveRecordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204g"

  val inactiveGoodsItemRecord: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = inactiveRecordId,
    traderRef = "BAN001001",
    comcode = "10410101",
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
    active = false,
    toReview = false,
    reviewReason = Some("no reason"),
    declarable = "IMMI ready",
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  protected override val repository =
    new RecordsRepository(mongoComponent = mongoComponent)

  private def byRecordId(recordId: String): Bson = Filters.equal("_id", recordId)

  ".set" - {

    "must create a record when there is none" in {
      val setResult     = repository.saveRecords(testEori, Seq(sampleGoodsItemRecords)).futureValue
      val updatedRecord = find(byRecordId(sampleGoodsItemRecords.recordId)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual sampleGoodsItemRecords
    }

    "must update a record when there is one" in {
      repository.saveRecords(testEori, Seq(sampleGoodsItemRecords)).futureValue
      val modifiedGoodsItemRecords = sampleGoodsItemRecords.copy(ukimsNumber = Some("new-ukims"))
      val expectedGoodsItemRecords = sampleGoodsItemRecords.copy(ukimsNumber = Some("new-ukims"))

      val saveResult    = repository.saveRecords(testEori, Seq(modifiedGoodsItemRecords)).futureValue
      val updatedRecord = find(byRecordId(sampleGoodsItemRecords.recordId)).futureValue.headOption.value

      saveResult mustEqual true
      updatedRecord mustEqual expectedGoodsItemRecords
    }
  }

  ".get" - {

    "when there is a record for this recordId it must get the record" in {
      insert(sampleGoodsItemRecords).futureValue
      val result = repository.get(testEori, testrecordId).futureValue
      result.value mustEqual sampleGoodsItemRecords
    }

    "when there is no active record for this recordId it must return None" in {
      insert(inactiveGoodsItemRecord).futureValue
      repository.get(testEori, testrecordId).futureValue must not be defined
    }

    "when there is no record for this recordId it must return None" in {
      repository.get(testEori, "recordId that does not exist").futureValue must not be defined
    }

    "when there are records for this eori it must return the active count" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3", active = false)).futureValue

      val result = repository.getCount(sampleGoodsItemRecords.eori).futureValue
      result mustEqual 2
    }

    "when there are no records for this eori it must return 0" in {
      repository.getCount(sampleGoodsItemRecords.eori).futureValue mustEqual 0
    }

    "when there are 8 records for this eori it must return the records and it asks for page 1 of size 5" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecords.eori, Some(1), Some(5)).futureValue
      result.size mustEqual 5
    }

    "when there are 8 records for this eori it must return the records and it asks for page 2 of size 5" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecords.eori, Some(2), Some(5)).futureValue
      result.size mustEqual 3
    }

    "when there are 8 records for this eori it must return empty Array and it asks for page 3 of size 5" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecords.eori, Some(3), Some(5)).futureValue
      result.size mustEqual 0
    }

    "when there are 8 records for this eori but only 2 of them are active: false it must return 2 records and it asks for page 1 of size 5" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3", active = false)).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4", active = false)).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5", active = false)).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "6", active = false)).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7", active = false)).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "8", active = false)).futureValue

      val result = repository.getMany(sampleGoodsItemRecords.eori, Some(1), Some(5)).futureValue
      result.size mustEqual 2
    }

    "when there are no records for this eori it must return empty Array" in {
      val result = repository.getMany(sampleGoodsItemRecords.eori, None, None).futureValue
      result.size mustEqual 0
    }

    "when there are 8 records for this eori it must get the last updated" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue

      val result = repository.getLatest(sampleGoodsItemRecords.eori).futureValue
      result.value.recordId mustEqual latestRecordId
    }

    "when there are no records for this eori it must return None" in {
      repository.getLatest(sampleGoodsItemRecords.eori).futureValue mustEqual None
    }
  }

  ".filter when exactMatch is true" - {

    val exactMatchIsTrue = true

    "when there are records for this eori but no searchTerm and field is passed it must return an empty sequence" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue

      val result =
        repository.filterRecords(sampleGoodsItemRecords.eori, None, None, exactMatch = exactMatchIsTrue).futureValue
      result mustEqual Seq.empty
    }

    "when there are records for this eori but no searchTerm is passed, but field is passed it must return an empty sequence" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue

      val result = repository
        .filterRecords(sampleGoodsItemRecords.eori, None, Some(traderRefSearchField), exactMatch = exactMatchIsTrue)
        .futureValue
      result mustEqual Seq.empty
    }

    "when there are 8 records and 2 matching traderRef searchTerm for this eori and the field is passed it must return a record of size 2" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(traderRefSearchTerm),
          Some(traderRefSearchField),
          exactMatch = exactMatchIsTrue
        )
        .futureValue
      result.size mustEqual 2
      result.headOption.value.traderRef mustEqual traderRefSearchTerm
    }

    "when there are 8 records and 2 matching traderRef searchTerm for this eori and the field is passed but only 1 is active it must return a record of size 2" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4", active = false)).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6", active = false)).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(traderRefSearchTerm),
          Some(traderRefSearchField),
          exactMatch = exactMatchIsTrue
        )
        .futureValue
      result.size mustEqual 1
      result.headOption.value.traderRef mustEqual traderRefSearchTerm
    }

    "when there are 8 records and 2 matching goodsDescription searchTerm for this eori and the field is passed it must return a record of size 2" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(goodsDescriptionSearchTerm),
          Some(goodsDescriptionSearchField),
          exactMatch = exactMatchIsTrue
        )
        .futureValue
      result.size mustEqual 2
      result.headOption.value.goodsDescription mustEqual goodsDescriptionSearchTerm
    }

    "when there are 8 records and 4 matching comCode searchTerm for this eori and the field is passed it must return a record of size 4" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(comCodeSearchTerm),
          Some(comCodeSearchField),
          exactMatch = exactMatchIsTrue
        )
        .futureValue
      result.size mustEqual 4
      result.headOption.value.comcode mustEqual comCodeSearchTerm
    }

    "when there are 8 records and 4 matching comCode searchTerm for this eori and the field is not passed it must return a record of size 4" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(sampleGoodsItemRecords.eori, Some(comCodeSearchTerm), None, exactMatch = exactMatchIsTrue)
        .futureValue
      result.size mustEqual 4
      result.headOption.value.comcode mustEqual comCodeSearchTerm
    }
  }

  ".filter when exactMatch is false" - {

    val exactMatchIsFalse = false

    "when there are records for this eori but no searchTerm and field is passed it must return an empty sequence" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue

      val result =
        repository.filterRecords(sampleGoodsItemRecords.eori, None, None, exactMatch = exactMatchIsFalse).futureValue
      result mustEqual Seq.empty
    }

    "when there are records for this eori but no searchTerm is passed, but field is passed it must return an empty sequence" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue

      val result = repository
        .filterRecords(sampleGoodsItemRecords.eori, None, Some(traderRefSearchField), exactMatch = exactMatchIsFalse)
        .futureValue
      result mustEqual Seq.empty
    }

    "when there are 8 records and 2 matching traderRef searchTerm for this eori and the field is passed it must return a record of size 4" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(traderRefPartialSearchTerm),
          Some(traderRefSearchField),
          exactMatch = exactMatchIsFalse
        )
        .futureValue
      result.size mustEqual 4
      result.headOption.value.traderRef.contains(traderRefPartialSearchTerm)
    }

    "when there are 8 records and 4 matching traderRef searchTerm for this eori and the field is passed but only 3 is active it must return a record of size 3" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4", active = false)).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6", active = false)).futureValue
      insert(inactiveGoodsItemRecord.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(traderRefPartialSearchTerm),
          Some(traderRefSearchField),
          exactMatch = exactMatchIsFalse
        )
        .futureValue
      result.size mustEqual 3
      result.headOption.value.traderRef.contains(traderRefPartialSearchTerm)
    }

    "when there are 8 records and 2 matching goodsDescription searchTerm for this eori and the field is passed it must return a record of size 4" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(goodsDescriptionPartialSearchTerm),
          Some(goodsDescriptionSearchField),
          exactMatch = exactMatchIsFalse
        )
        .futureValue
      result.size mustEqual 4
      result.headOption.value.goodsDescription.contains(goodsDescriptionPartialSearchTerm)
    }

    "when there are 8 records and 4 matching comCode searchTerm for this eori and the field is passed it must return a record of size 4" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(comCodePartialSearchTerm),
          Some(comCodeSearchField),
          exactMatch = exactMatchIsFalse
        )
        .futureValue
      result.size mustEqual 4
      result.headOption.value.comcode.contains(comCodePartialSearchTerm)
    }

    "when there are 8 records and 4 matching comCode searchTerm for this eori and the field is not passed it must return a record of size 4" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(
          sampleGoodsItemRecords.eori,
          Some(comCodePartialSearchTerm),
          None,
          exactMatch = exactMatchIsFalse
        )
        .futureValue
      result.size mustEqual 4
      result.headOption.value.comcode.contains(comCodePartialSearchTerm)
    }
  }
}
