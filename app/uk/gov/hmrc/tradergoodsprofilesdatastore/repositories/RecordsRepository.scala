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
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GoodsItemRecord

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecordsRepository @Inject() (
  mongoComponent: MongoComponent,
  config: DataStoreAppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[GoodsItemRecord](
      collectionName = "goodsItemRecords",
      mongoComponent = mongoComponent,
      domainFormat = GoodsItemRecord.goodsItemRecordsFormat,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("recordId"),
          IndexOptions().name("recordId")
        )
      )
    ) {

  private def byRecordId(recordId: String): Bson  = Filters.equal("recordId", recordId)
  private def byEori(eori: String): Bson          = Filters.equal("eori", eori)
  private def byEoriAndActive(eori: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("active", false))

  private def byLatest: Bson = Sorts.descending("updatedDateTime")

  private def byTraderRef(traderRef: String): Bson = Filters.equal("traderRef", traderRef)

  private def byGoodsDescription(goodsDescription: String): Bson = Filters.equal("goodsDescription", goodsDescription)

  private def byCountryOfOrigin(countryOfOrigin: String): Bson = Filters.equal("countryOfOrigin", countryOfOrigin)

  private def byCountryOfOriginOrGoodsDescriptionOrTraderRef(value: String): Bson =
    Filters.or(
      Filters.equal("traderRef", value),
      Filters.equal("goodsDescription", value),
      Filters.equal("countryOfOrigin", value)
    )

  def saveRecords(records: Seq[GoodsItemRecord]): Future[Boolean] =
    Future
      .sequence(records.map { record =>
        collection
          .replaceOne(
            filter = byRecordId(record.recordId),
            replacement = record,
            options = ReplaceOptions().upsert(true)
          )
          .toFuture()
      })
      .map(_ => true)

  def get(recordId: String): Future[Option[GoodsItemRecord]] =
    collection
      .find[GoodsItemRecord](byRecordId(recordId))
      .headOption()

  def getMany(eori: String, pageOpt: Option[Int], sizeOpt: Option[Int]): Future[Seq[GoodsItemRecord]] = {
    val size = sizeOpt.getOrElse(config.pageSize)
    val page = pageOpt.getOrElse(config.startingPage)
    val skip = (page - 1) * size
    collection
      .find[GoodsItemRecord](byEori(eori))
      .sort(byLatest)
      .limit(size)
      .skip(skip)
      .toFuture()
  }

  def getCount(eori: String): Future[Long] =
    collection
      .countDocuments(byEori(eori))
      .toFuture()

  def getLatest(eori: String): Future[Option[GoodsItemRecord]] =
    collection
      .find[GoodsItemRecord](byEori(eori))
      .sort(byLatest)
      .limit(1)
      .headOption()

  def delete(recordId: String): Future[Boolean] =
    collection
      .deleteOne(
        filter = byRecordId(recordId)
      )
      .toFuture()
      .map(_.getDeletedCount > 0)

  def deleteInactive(eori: String): Future[Long] =
    collection
      .deleteMany(
        filter = byEoriAndActive(eori)
      )
      .toFuture()
      .map(_.getDeletedCount)

  def saveRecord(record: GoodsItemRecord): Future[Boolean] =
    collection
      .replaceOne(
        filter = byRecordId(record.recordId),
        replacement = record,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)

  def update(recordId: String, updateRequest: UpdateRecordRequest): Future[Option[GoodsItemRecord]] =
    get(recordId).flatMap {
      case Some(existingRecord) =>
        val updatedRecord = existingRecord.copy(
          traderRef = updateRequest.traderRef.getOrElse(existingRecord.traderRef),
          comcode = updateRequest.comcode.getOrElse(existingRecord.comcode),
          goodsDescription = updateRequest.goodsDescription.getOrElse(existingRecord.goodsDescription),
          countryOfOrigin = updateRequest.countryOfOrigin.getOrElse(existingRecord.countryOfOrigin),
          category = updateRequest.category.getOrElse(existingRecord.category),
          assessments = updateRequest.assessments.orElse(existingRecord.assessments),
          supplementaryUnit = updateRequest.supplementaryUnit.orElse(existingRecord.supplementaryUnit),
          measurementUnit = updateRequest.measurementUnit.orElse(existingRecord.measurementUnit),
          comcodeEffectiveFromDate =
            updateRequest.comcodeEffectiveFromDate.getOrElse(existingRecord.comcodeEffectiveFromDate),
          comcodeEffectiveToDate = updateRequest.comcodeEffectiveToDate.orElse(existingRecord.comcodeEffectiveToDate)
        )

        saveRecord(updatedRecord).map { case true =>
          Some(updatedRecord)
        }

      case None => Future.successful(None)
    }

  def filterRecords(eori: String, searchTerm: Option[String], field: Option[String]): Future[Seq[GoodsItemRecord]] =
    field match {
      case Some(value) =>
        searchTerm match {
          case Some("traderRef")        =>
            collection
              .find[GoodsItemRecord](byTraderRef(value))
              .sort(byLatest)
              .toFuture()
          case Some("goodsDescription") =>
            collection
              .find[GoodsItemRecord](byGoodsDescription(value))
              .sort(byLatest)
              .toFuture()
          case Some("countryOfOrigin")  =>
            collection
              .find[GoodsItemRecord](byCountryOfOrigin(value))
              .sort(byLatest)
              .toFuture()
          case None                     =>
            collection
              .find[GoodsItemRecord](byCountryOfOriginOrGoodsDescriptionOrTraderRef(value))
              .sort(byLatest)
              .toFuture()
        }
      case None        => getMany(eori, None, None)
    }
}
