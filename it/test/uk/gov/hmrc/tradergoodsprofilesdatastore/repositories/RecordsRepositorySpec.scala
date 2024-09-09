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
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.{FakeRetireFileAction, FakeStoreLatestAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{RetireFileAction, StoreLatestAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Assessment, Condition, GoodsItemRecord}

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

  val sampleUpdateRequest: UpdateRecordRequest = UpdateRecordRequest(
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
    declarable = "IMMI ready",
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
    declarable = "IMMI ready",
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
    declarable = "IMMI ready",
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].to(mongoComponent),
      bind[StoreLatestAction].to[FakeStoreLatestAction],
      bind[RetireFileAction].to[FakeRetireFileAction]
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

      val result = repository.getMany(sampleGoodsItemRecord.eori, Some(1), Some(5)).futureValue
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

      val result = repository.getMany(sampleGoodsItemRecord.eori, Some(2), Some(5)).futureValue
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

      val result = repository.getMany(sampleGoodsItemRecord.eori, Some(3), Some(5)).futureValue
      result.size mustEqual 0
    }

    "when there are no records for this eori it must return empty Array" in {
      val result = repository.getMany(sampleGoodsItemRecord.eori, None, None).futureValue
      result.size mustEqual 0
    }

    mustPreserveMdc(repository.getMany(sampleGoodsItemRecord.eori, None, None))
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

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      val ec = app.injector.instanceOf[ExecutionContext]

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }(ec).futureValue
    }
}
