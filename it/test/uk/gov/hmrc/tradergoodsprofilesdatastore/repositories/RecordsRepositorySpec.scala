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
    comcode = "10410100",
    adviceStatus = "Not requested",
    goodsDescription = "Organic bananas",
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

  private val traderRefSearchTerm            = "BAN001002"
  private val traderRefSearchField           = "traderRef"
  private val goodsDescriptionRefSearchTerm  = "Tomatoes"
  private val goodsDescriptionRefSearchField = "goodsDescription"
  private val countryOfOriginSearchTerm      = "AU"
  private val countryOfOriginSearchField     = "countryOfOrigin"

  private val latestRecordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204p"

  val latestGoodsItemRecords: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = latestRecordId,
    traderRef = "BAN001001",
    comcode = "10410100",
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

  val inactiveGoodsItemRecords: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = inactiveRecordId,
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

    "when there is no record for this recordId it must return None" in {
      repository.get(testEori, "recordId that does not exist").futureValue must not be defined
    }

    "when there are records for this eori it must return the count" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue

      val result = repository.getCount(sampleGoodsItemRecords.eori).futureValue
      result mustEqual 3
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

    "when there are records for this eori and for search term in trader reference it must return the count" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue

      val result = repository.getCount(sampleGoodsItemRecords.eori, "BAN001001").futureValue
      result mustEqual 3

      val partialSearchResult = repository.getCount(sampleGoodsItemRecords.eori, "BAN00").futureValue
      partialSearchResult mustEqual 3
    }

    "when there are records for this eori and for search term in commodity code it must return the count" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue

      val result = repository.getCount(sampleGoodsItemRecords.eori, "10410100").futureValue
      result mustEqual 3

      val partialSearchResult = repository.getCount(sampleGoodsItemRecords.eori, "104101").futureValue
      partialSearchResult mustEqual 3
    }

    "when there are records for this eori and for search term in goods description it must return the count" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue

      val result = repository.getCount(sampleGoodsItemRecords.eori, "Organic bananas").futureValue
      result mustEqual 3

      val partialSearchResult = repository.getCount(sampleGoodsItemRecords.eori, "bananas").futureValue
      partialSearchResult mustEqual 3
    }

    "when there are no records for this eori and for search term it must return 0" in {
      repository
        .getCount(sampleGoodsItemRecords.eori, "value not match with trader ref, comCode or goods description")
        .futureValue mustEqual 0
    }

    "when there are 8 records for this eori and for search term in trader reference it must return the records and it asks for page 2 of size 5" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecords.eori, "BAN001001", Some(2), Some(5)).futureValue
      result.size mustEqual 4

      val partialSearchResult = repository.getMany(sampleGoodsItemRecords.eori, "BAN00", Some(2), Some(5)).futureValue
      partialSearchResult.size mustEqual 4
    }

    "when there are 8 records for this eori and for search term in commodity code it must return the records and it asks for page 2 of size 5" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecords.eori, "10410100", Some(2), Some(5)).futureValue
      result.size mustEqual 4

      val partialSearchResult = repository.getMany(sampleGoodsItemRecords.eori, "104101", Some(2), Some(5)).futureValue
      partialSearchResult.size mustEqual 4
    }

    "when there are 8 records for this eori and for search term in goods description it must return the records and it asks for page 2 of size 5" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecords.eori, "Organic bananas", Some(2), Some(5)).futureValue
      result.size mustEqual 4

      val partialSearchResult = repository.getMany(sampleGoodsItemRecords.eori, "bananas", Some(2), Some(5)).futureValue
      partialSearchResult.size mustEqual 4
    }

    "when there are 8 records for this eori and for search term it must return empty Array and it asks for page 3 of size 5" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecords.eori, "Organic bananas", Some(3), Some(5)).futureValue
      result.size mustEqual 0
    }

    "when there are no records for this eori and for search term it must return empty Array" in {
      val result = repository.getMany(sampleGoodsItemRecords.eori, None, None).futureValue
      result.size mustEqual 0
    }
  }

  ".filter" - {
    "when there are records for this eori but no searchTerm and field is passed it must return an empty sequence" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue

      val result = repository.filterRecords(sampleGoodsItemRecords.eori, None, None).futureValue
      result mustEqual Seq.empty
    }

    "when there are records for this eori but no searchTerm is passed, but field is passed it must return an empty sequence" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue

      val result = repository.filterRecords(sampleGoodsItemRecords.eori, None, Some(traderRefSearchField)).futureValue
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
        .filterRecords(sampleGoodsItemRecords.eori, Some(traderRefSearchTerm), Some(traderRefSearchField))
        .futureValue
      result.size mustEqual 2
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
          Some(goodsDescriptionRefSearchTerm),
          Some(goodsDescriptionRefSearchField)
        )
        .futureValue
      result.size mustEqual 2
      result.headOption.value.goodsDescription mustEqual goodsDescriptionRefSearchTerm
    }

    "when there are 8 records and 4 matching countryOfOrigin searchTerm for this eori and the field is passed it must return a record of size 4" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(sampleGoodsItemRecords.eori, Some(countryOfOriginSearchTerm), Some(countryOfOriginSearchField))
        .futureValue
      result.size mustEqual 4
      result.headOption.value.countryOfOrigin mustEqual countryOfOriginSearchTerm
    }

    "when there are 8 records and 4 matching countryOfOrigin searchTerm for this eori and the field is not passed it must return a record of size 4" in {
      insert(sampleGoodsItemRecords).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecords2.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "5")).futureValue
      insert(latestGoodsItemRecords).futureValue
      insert(latestGoodsItemRecords.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecords.copy(recordId = "7")).futureValue

      val result = repository
        .filterRecords(sampleGoodsItemRecords.eori, Some(countryOfOriginSearchTerm), None)
        .futureValue
      result.size mustEqual 4
      result.headOption.value.countryOfOrigin mustEqual countryOfOriginSearchTerm
    }
  }
}
