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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GoodsItemRecord
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{localPageSize, localStartingPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecordsRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[GoodsItemRecord](
      collectionName = "goodsItemRecords",
      mongoComponent = mongoComponent,
      domainFormat = GoodsItemRecord.goodsItemRecordsMongoFormat,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("recordId"),
          IndexOptions().name("recordId")
        )
      )
    ) {

  private def byEori(eori: String): Bson          = Filters.equal("eori", eori)
  private def byEoriAndActive(eori: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("active", true))

  private def byEoriAndRecordIdAndActive(eori: String, recordId: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("_id", recordId), Filters.equal("active", true))

  private def byEoriAndRecordId(eori: String, recordId: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("_id", recordId))

  private def byLatest: Bson = Sorts.descending("updatedDateTime")

  private def byField(field: String, searchTerm: String): Bson = Filters.equal(field, searchTerm)

  private def byCountryOfOriginOrGoodsDescriptionOrTraderRef(value: String): Bson =
    Filters.or(
      Filters.equal("traderRef", value),
      Filters.equal("goodsDescription", value),
      Filters.equal("countryOfOrigin", value)
    )

  def saveRecords(eori: String, records: Seq[GoodsItemRecord]): Future[Boolean] =
    Future
      .sequence(records.map { record =>
        collection
          .replaceOne(
            filter = byEoriAndRecordId(eori, record.recordId),
            replacement = record,
            options = ReplaceOptions().upsert(true)
          )
          .toFuture()
      })
      .map(_ => true)

  def get(eori: String, recordId: String): Future[Option[GoodsItemRecord]] =
    collection
      .find[GoodsItemRecord](byEoriAndRecordIdAndActive(eori, recordId))
      .headOption()

  def getMany(eori: String, pageOpt: Option[Int], sizeOpt: Option[Int]): Future[Seq[GoodsItemRecord]] = {
    val size = sizeOpt.getOrElse(localPageSize)
    val page = pageOpt.getOrElse(localStartingPage)
    val skip = (page - 1) * size
    collection
      .find[GoodsItemRecord](byEoriAndActive(eori))
      .sort(byLatest)
      .limit(size)
      .skip(skip)
      .toFuture()
  }

  def getCount(eori: String): Future[Long] =
    collection
      .countDocuments(byEoriAndActive(eori))
      .toFuture()

  def getLatest(eori: String): Future[Option[GoodsItemRecord]] =
    collection
      .find[GoodsItemRecord](byEori(eori))
      .sort(byLatest)
      .limit(1)
      .headOption()

  def filterRecords(eori: String, searchTerm: Option[String], field: Option[String]): Future[Seq[GoodsItemRecord]] =
    searchTerm match {
      case Some(value) =>
        field match {
          case Some(searchField) =>
            collection
              .find[GoodsItemRecord](
                Filters.and(
                  byEoriAndActive(eori),
                  byField(searchField, value)
                )
              )
              .sort(byLatest)
              .toFuture()
          case _                 =>
            collection
              .find[GoodsItemRecord](
                Filters.and(
                  byEoriAndActive(eori),
                  byCountryOfOriginOrGoodsDescriptionOrTraderRef(value)
                )
              )
              .sort(byLatest)
              .toFuture()
        }
      case None        => Future.successful(Seq.empty)
    }
}
