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

import org.mongodb.scala.model.Filters
import org.scalactic.source.Position
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.slf4j.MDC
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeStoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.StoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.PatchRecordRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Assessment, Condition, GoodsItemRecord}
import org.mongodb.scala.ObservableFuture
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class RecordsRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[GoodsItemRecord]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

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

  val sampleUpdateRequest: PatchRecordRequest = PatchRecordRequest(
    actorId = "GB098765432112",
    traderRef = Some("updated-reference")
  )

  val testEori                               = "GB123456789001"
  val sampleGoodsItemRecord: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = testrecordId,
    traderRef = "BAN001001",
    comcode = "10410100",
    adviceStatus = "Not requested",
    goodsDescription = "Organic bananas",
    countryOfOrigin = "EC",
    category = Some(3),
    assessments = Some(Seq(sampleAssessment)),
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

  val sampleGoodsItemRecord2: GoodsItemRecord = GoodsItemRecord(
    eori = "GB123456789001",
    actorId = "GB098765432112",
    recordId = testrecordId,
    traderRef = "BAN001002",
    comcode = "20410101",
    adviceStatus = "Not requested",
    goodsDescription = "Apples",
    countryOfOrigin = "AU",
    category = Some(3),
    assessments = Some(Seq(sampleAssessment)),
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

  val sampleGoodsItemRecordWithSpecialCharacters: GoodsItemRecord = GoodsItemRecord(
    eori = "GB123456789001",
    actorId = "GB098765432112",
    recordId = testrecordId,
    traderRef = "BAN001002",
    comcode = "20410101",
    adviceStatus = "Not requested",
    goodsDescription = "Apples with ^$*+?.(){}[]",
    countryOfOrigin = "AU",
    category = Some(3),
    assessments = Some(Seq(sampleAssessment)),
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

  val latestGoodsItemRecord: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = latestRecordId,
    traderRef = "BAN001003",
    comcode = "30410102",
    adviceStatus = "Not requested",
    goodsDescription = "Tomatoes",
    countryOfOrigin = "AU",
    category = Some(3),
    assessments = Some(Seq(sampleAssessment)),
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
    category = Some(3),
    assessments = Some(Seq(sampleAssessment)),
    supplementaryUnit = Some(500),
    measurementUnit = Some("square meters(m^2)"),
    comcodeEffectiveFromDate = Instant.parse("2024-10-12T16:12:34Z"),
    comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
    version = 1,
    active = false,
    toReview = false,
    reviewReason = Some("no reason"),
    declarable = "IMMI Ready",
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].to(mongoComponent),
      bind[StoreLatestAction].to[FakeStoreLatestAction]
    )
    .build()

  protected override val repository: RecordsRepository =
    app.injector.instanceOf[RecordsRepository]

  ".getCount" - {

    "when there are records for this eori" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue

      val result = repository.getCount(sampleGoodsItemRecord.eori).futureValue
      result mustEqual 2
    }

    "when there are no records for this eori it must return 0" in {
      repository.getCount(sampleGoodsItemRecord.eori).futureValue mustEqual 0
    }

    mustPreserveMdc(repository.getCount(sampleGoodsItemRecord.eori))
  }

  ".getMany" - {

    "when there are 8 records for this eori it must return the records and it asks for page 1 of size 5" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecord.eori, 1, 5).futureValue
      result.size mustEqual 5
    }

    "when there are 8 records for this eori it must return the records and it asks for page 2 of size 5" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecord.eori, 2, 5).futureValue
      result.size mustEqual 3
    }

    "when there are 8 records for this eori it must return empty Array and it asks for page 3 of size 5" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "3")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "4")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "6")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "8")).futureValue

      val result = repository.getMany(sampleGoodsItemRecord.eori, 3, 5).futureValue
      result.size mustEqual 0
    }

    "when there are no records for this eori it must return empty Array" in {
      val result = repository.getMany(sampleGoodsItemRecord.eori, 0, 0).futureValue
      result.size mustEqual 0
    }
    MDC.put("myKey", "foo")
    whenReady(repository.getMany(sampleGoodsItemRecord.eori, 0, 0)) { _ =>
      MDC.get("myKey") mustBe "foo"
    }

  }

  ".filterRecords" - {

    "when exactMatch is true" - {

      "when there are records for this eori but no searchTerm and field is passed it must return an empty sequence" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue

        val result =
          repository.filterRecords(sampleGoodsItemRecord.eori, None, None, exactMatch = true).futureValue
        result mustEqual Seq.empty
      }

      "when there are records for this eori but no searchTerm is passed, but field is passed it must return an empty sequence" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue

        val result = repository
          .filterRecords(sampleGoodsItemRecord.eori, None, Some(traderRefSearchField), exactMatch = true)
          .futureValue
        result mustEqual Seq.empty
      }

      "when there are 8 records and 2 matching traderRef searchTerm for this eori and the field is passed it must return a record of size 2" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some(traderRefSearchTerm),
            Some(traderRefSearchField),
            exactMatch = true
          )
          .futureValue
        result.size mustEqual 2
        result.headOption.value.traderRef mustEqual traderRefSearchTerm
      }

      "when there are 8 records and 2 matching goodsDescription searchTerm for this eori and the field is passed it must return a record of size 2" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some(goodsDescriptionSearchTerm),
            Some(goodsDescriptionSearchField),
            exactMatch = true
          )
          .futureValue
        result.size mustEqual 2
        result.headOption.value.goodsDescription mustEqual goodsDescriptionSearchTerm
      }

      "when there are 8 records and 4 matching comCode searchTerm for this eori and the field is passed it must return a record of size 4" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some(comCodeSearchTerm),
            Some(comCodeSearchField),
            exactMatch = true
          )
          .futureValue
        result.size mustEqual 4
        result.headOption.value.comcode mustEqual comCodeSearchTerm
      }

      "when there are 8 records and 4 matching comCode searchTerm for this eori and the field is not passed it must return a record of size 4" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(sampleGoodsItemRecord.eori, Some(comCodeSearchTerm), None, exactMatch = true)
          .futureValue
        result.size mustEqual 4
        result.headOption.value.comcode mustEqual comCodeSearchTerm
      }

      "return case insensitive matches when searching on the Trader Reference field" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some("ban001002"),
            Some(traderRefSearchField),
            exactMatch = true
          )
          .futureValue
        result.size mustEqual 2
        result.headOption.value.traderRef mustEqual traderRefSearchTerm
        result.reverse.headOption.value.traderRef mustEqual traderRefSearchTerm
      }

    }

    "exactMatch is false" - {

      "when there are records for this eori but no searchTerm and field is passed it must return an empty sequence" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue

        val result =
          repository.filterRecords(sampleGoodsItemRecord.eori, None, None, exactMatch = false).futureValue
        result mustEqual Seq.empty
      }

      "when there are records for this eori but no searchTerm is passed, but field is passed it must return an empty sequence" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue

        val result = repository
          .filterRecords(sampleGoodsItemRecord.eori, None, Some(traderRefSearchField), exactMatch = false)
          .futureValue
        result mustEqual Seq.empty
      }

      "when there are 8 records and 2 matching traderRef searchTerm for this eori and the field is passed it must return a record of size 4" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some(traderRefPartialSearchTerm),
            Some(traderRefSearchField),
            exactMatch = false
          )
          .futureValue
        result.size mustEqual 4
        result.headOption.value.traderRef.contains(traderRefPartialSearchTerm)
      }

      "when there are 8 records and 2 matching goodsDescription searchTerm for this eori and the field is passed it must return a record of size 4" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some(goodsDescriptionPartialSearchTerm),
            Some(goodsDescriptionSearchField),
            exactMatch = false
          )
          .futureValue
        result.size mustEqual 4
        result.headOption.value.goodsDescription.contains(goodsDescriptionPartialSearchTerm)
      }

      "when there are 8 records and 4 matching comCode searchTerm for this eori and the field is passed it must return a record of size 4" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some(comCodePartialSearchTerm),
            Some(comCodeSearchField),
            exactMatch = false
          )
          .futureValue
        result.size mustEqual 4
        result.headOption.value.comcode.contains(comCodePartialSearchTerm)
      }

      "when there are 8 records and 4 matching comCode searchTerm for this eori and the field is not passed it must return a record of size 4" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "3")).futureValue
        insert(sampleGoodsItemRecord2.copy(recordId = "4")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "5")).futureValue
        insert(latestGoodsItemRecord).futureValue
        insert(latestGoodsItemRecord.copy(recordId = "6")).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "7")).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some(comCodePartialSearchTerm),
            None,
            exactMatch = false
          )
          .futureValue
        result.size mustEqual 4
        result.headOption.value.comcode.contains(comCodePartialSearchTerm)
      }

      "when there is 1 record with special characters and 0 matching goodsDescription searchTerm for this eori and the field is not passed it must return a record of size 0" in {
        insert(sampleGoodsItemRecord).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecord.eori,
            Some("*"),
            None,
            exactMatch = false
          )
          .futureValue

        result mustEqual Seq.empty
      }

      "when there is 1 record with special characters and 1 matching goodsDescription searchTerm for this eori and the field is not passed it must return a record of size 1" in {
        insert(sampleGoodsItemRecordWithSpecialCharacters).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecordWithSpecialCharacters.eori,
            Some("^$*+?.(){}[]"),
            None,
            exactMatch = false
          )
          .futureValue
        result.size mustEqual 1
        result.headOption.value.goodsDescription.contains("^$*+?.(){}[]")
      }

      "when there is 1 record with special character * and 1 matching goodsDescription searchTerm for this eori and the field is not passed it must return a record of size 1" in {
        insert(sampleGoodsItemRecordWithSpecialCharacters).futureValue

        val result = repository
          .filterRecords(
            sampleGoodsItemRecordWithSpecialCharacters.eori,
            Some("*"),
            None,
            exactMatch = false
          )
          .futureValue
        result.size mustEqual 1
        result.headOption.value.goodsDescription.contains("*")
      }
    }

    mustPreserveMdc(
      repository.filterRecords(sampleGoodsItemRecordWithSpecialCharacters.eori, Some("*"), None, exactMatch = false)
    )
  }

  ".updateRecords" - {

    "must insert, update, or delete the relevant records" in {

      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue

      val updatedRecords = Seq(
        sampleGoodsItemRecord.copy(toReview = true),
        sampleGoodsItemRecord.copy(recordId = "2", active = false),
        sampleGoodsItemRecord.copy(recordId = "3", toReview = true)
      )

      repository.updateRecords(sampleGoodsItemRecord.eori, updatedRecords).futureValue

      val results = repository.collection.find().toFuture().futureValue

      results.length mustBe 2
      results must contain only (
        sampleGoodsItemRecord.copy(toReview = true),
        sampleGoodsItemRecord.copy(recordId = "3", toReview = true)
      )
    }

    mustPreserveMdc(repository.updateRecords(sampleGoodsItemRecord.eori, Seq.empty))
  }

  ".delete" - {
    "when there is a records for this eori" in {
      insert(sampleGoodsItemRecord).futureValue

      val result = repository.deleteRecordsByEori(sampleGoodsItemRecord.eori).futureValue

      result mustEqual 1
    }

    "when there are multiple records with different eori's" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2", eori = "GB123456789002")).futureValue

      val result = repository.deleteRecordsByEori(sampleGoodsItemRecord.eori).futureValue

      result mustEqual 1
    }

    "when there is a no recordsSummary for this eori it must return 0" in {
      val result = repository.deleteRecordsByEori(sampleGoodsItemRecord.eori).futureValue

      result mustEqual 0
    }

    mustPreserveMdc(repository.deleteRecordsByEori(sampleGoodsItemRecord.eori))
  }

  ".searchTermFilter" - {

    "when searchTerm is None" in {
      val result = repository.searchTermFilter(None)
      result mustEqual Filters.exists("traderRef")
    }

    "when searchTerm is Some" - {

      "should return a filter that matches traderRef, goodsDescription, or comcode" in {
        val searchTerm     = "test"
        val result         = repository.searchTermFilter(Some(searchTerm))
        val expectedFilter = Filters.or(
          Filters.regex("traderRef", searchTerm, "i"),
          Filters.regex("goodsDescription", searchTerm, "i"),
          Filters.regex("comcode", searchTerm, "i")
        )
        result mustEqual expectedFilter
      }

      "should escape special characters in the searchTerm" in {
        val searchTerm        = "^$*+?.(){}[]"
        val escapedSearchTerm = "\\^\\$\\*\\+\\?\\.\\(\\)\\{\\}\\[\\]"
        val result            = repository.searchTermFilter(Some(searchTerm))
        val expectedFilter    = Filters.or(
          Filters.regex("traderRef", escapedSearchTerm, "i"),
          Filters.regex("goodsDescription", escapedSearchTerm, "i"),
          Filters.regex("comcode", escapedSearchTerm, "i")
        )
        result mustEqual expectedFilter
      }
    }
  }

  ".countryOfOriginFilter" - {

    "when countryOfOrigin is None" in {
      val result = repository.countryOfOriginFilter(None)
      result mustEqual Filters.exists("countryOfOrigin")
    }

    "when countryOfOrigin is Some" in {
      val countryOfOrigin = "AU"
      val result          = repository.countryOfOriginFilter(Some(countryOfOrigin))
      val expectedFilter  = Filters.equal("countryOfOrigin", countryOfOrigin)
      result mustEqual expectedFilter
    }
  }

  ".declarableFilter" - {

    "when all options are None" in {
      val result = repository.declarableFilter(None, None, None)
      result mustEqual Filters.exists("declarable")
    }

    "when IMMIReady is Some(true)" in {
      val result         = repository.declarableFilter(Some(true), None, None)
      val expectedFilter = Filters.or(Filters.equal("declarable", "IMMI Ready"))
      result mustEqual expectedFilter
    }

    "when notReadyForIMMI is Some(true)" in {
      val result         = repository.declarableFilter(None, Some(true), None)
      val expectedFilter = Filters.or(Filters.equal("declarable", "Not ready for IMMI"))
      result mustEqual expectedFilter
    }

    "when actionNeeded is Some(true)" in {
      val result         = repository.declarableFilter(None, None, Some(true))
      val expectedFilter = Filters.or(Filters.equal("declarable", "Not Ready For Use"))
      result mustEqual expectedFilter
    }

    "when multiple options are Some(true)" in {
      val result         = repository.declarableFilter(Some(true), Some(true), None)
      val expectedFilter = Filters.or(
        Filters.equal("declarable", "IMMI Ready"),
        Filters.equal("declarable", "Not ready for IMMI")
      )
      result mustEqual expectedFilter
    }

    "when all options are Some(true)" in {
      val result         = repository.declarableFilter(Some(true), Some(true), Some(true))
      val expectedFilter = Filters.or(
        Filters.equal("declarable", "IMMI Ready"),
        Filters.equal("declarable", "Not ready for IMMI"),
        Filters.equal("declarable", "Not Ready For Use")
      )
      result mustEqual expectedFilter
    }
  }

  ".filterRecordsIteration" - {

    "when all filters are None" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2")).futureValue

      val result = repository
        .filterRecordsIteration(
          sampleGoodsItemRecord.eori,
          None,
          None,
          None,
          None,
          None
        )
        .futureValue

      result.size mustEqual 2
    }

    "when searchTerm filter is applied" - {
      "for traderRef" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2", traderRef = "test data")).futureValue

        val result = repository
          .filterRecordsIteration(
            sampleGoodsItemRecord.eori,
            Some("test"),
            None,
            None,
            None,
            None
          )
          .futureValue

        result.size mustEqual 1
        result.head.traderRef mustEqual "test data"
      }

      "for goodsDescription" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2", goodsDescription = "test data")).futureValue

        val result = repository
          .filterRecordsIteration(
            sampleGoodsItemRecord.eori,
            Some("test"),
            None,
            None,
            None,
            None
          )
          .futureValue

        result.size mustEqual 1
        result.head.goodsDescription mustEqual "test data"
      }

      "for comcode" in {
        insert(sampleGoodsItemRecord).futureValue
        insert(sampleGoodsItemRecord.copy(recordId = "2", comcode = "test data")).futureValue

        val result = repository
          .filterRecordsIteration(
            sampleGoodsItemRecord.eori,
            Some("test"),
            None,
            None,
            None,
            None
          )
          .futureValue

        result.size mustEqual 1
        result.head.comcode mustEqual "test data"
      }
    }

    "when countryOfOrigin filter is applied" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2", countryOfOrigin = "AU")).futureValue

      val result = repository
        .filterRecordsIteration(
          sampleGoodsItemRecord.eori,
          None,
          Some("AU"),
          None,
          None,
          None
        )
        .futureValue

      result.size mustEqual 1
      result.head.countryOfOrigin mustEqual "AU"
    }

    "when IMMIReady filter is applied" in {
      insert(sampleGoodsItemRecord.copy(declarable = "Not Ready For Use")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2", declarable = "IMMI Ready")).futureValue

      val result = repository
        .filterRecordsIteration(
          sampleGoodsItemRecord.eori,
          None,
          None,
          Some(true),
          None,
          None
        )
        .futureValue

      result.size mustEqual 1
      result.head.declarable mustEqual "IMMI Ready"
    }

    "when notReadyForIMMI filter is applied" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2", declarable = "Not ready for IMMI")).futureValue

      val result = repository
        .filterRecordsIteration(
          sampleGoodsItemRecord.eori,
          None,
          None,
          None,
          Some(true),
          None
        )
        .futureValue

      result.size mustEqual 1
      result.head.declarable mustEqual "Not ready for IMMI"
    }

    "when actionNeeded filter is applied" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2", declarable = "Not Ready For Use")).futureValue

      val result = repository
        .filterRecordsIteration(
          sampleGoodsItemRecord.eori,
          None,
          None,
          None,
          None,
          Some(true)
        )
        .futureValue

      result.size mustEqual 1
      result.head.declarable mustEqual "Not Ready For Use"
    }

    "when multiple filters are applied" in {
      insert(sampleGoodsItemRecord).futureValue
      insert(
        sampleGoodsItemRecord
          .copy(recordId = "2", goodsDescription = "Apples", countryOfOrigin = "AU", declarable = "IMMI Ready")
      ).futureValue

      val result = repository
        .filterRecordsIteration(
          sampleGoodsItemRecord.eori,
          Some("Apples"),
          Some("AU"),
          Some(true),
          None,
          None
        )
        .futureValue

      result.size mustEqual 1
    }
  }

  "isTraderReferenceUnique" - {

    "when the trader reference is unique" in {
      insert(sampleGoodsItemRecord).futureValue

      val result = repository.isTraderReferenceUnique(sampleGoodsItemRecord.eori, "uniqueRef").futureValue
      result mustEqual true
    }

    "must return false when trader reference with different case exists" in {
      insert(sampleGoodsItemRecord.copy(traderRef = "product")).futureValue

      val result = repository.isTraderReferenceUnique(sampleGoodsItemRecord.eori, "Product").futureValue
      result mustEqual false
    }

    "when the trader reference is not unique" in {
      insert(sampleGoodsItemRecord.copy(traderRef = "duplicateRef")).futureValue
      insert(sampleGoodsItemRecord.copy(recordId = "2", traderRef = "duplicateRef")).futureValue

      val result = repository.isTraderReferenceUnique(sampleGoodsItemRecord.eori, "duplicateRef").futureValue
      result mustEqual false
    }

    "when there are no records for the given eori" in {
      val result = repository.isTraderReferenceUnique("nonExistentEori", "anyRef").futureValue
      result mustEqual true
    }

    mustPreserveMdc(repository.isTraderReferenceUnique(sampleGoodsItemRecord.eori, "uniqueRef"))
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
