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
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.StringHelper.escapeRegexSpecialChars

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

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  private def byEoriAndActive(eori: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("active", true))

  private def byEoriAndInactive(eori: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("active", false))

  private def byEoriAndRecordId(eori: String, recordId: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("_id", recordId))

  private def byLatest: Bson = Sorts.descending("updatedDateTime")

  private def byField(field: String, searchTerm: String, exactMatch: Boolean): Bson =
    if (exactMatch)
      Filters.equal(field, searchTerm)
    else
      Filters.regex(field, searchTerm, "i")

  private def byComCodeOrGoodsDescriptionOrTraderRef(value: String, exactMatch: Boolean): Bson =
    if (exactMatch) {
      Filters.or(
        Filters.equal("traderRef", value),
        Filters.equal("goodsDescription", value),
        Filters.equal("comcode", value)
      )
    } else {
      val escapedSearchString = escapeRegexSpecialChars(value)
      val searchPattern       = s".*$escapedSearchString.*"
      Filters.or(
        Filters.regex("traderRef", searchPattern, "i"),
        Filters.regex("goodsDescription", searchPattern, "i"),
        Filters.regex("comcode", searchPattern, "i")
      )
    }

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

  def getCountWithInactive(eori: String): Future[Long] =
    collection
      .countDocuments(byEori(eori))
      .toFuture()

  def getLatest(eori: String): Future[Option[GoodsItemRecord]] =
    collection
      .find[GoodsItemRecord](byEori(eori))
      .sort(byLatest)
      .limit(1)
      .headOption()

  def filterRecords(
    eori: String,
    searchTerm: Option[String],
    field: Option[String],
    exactMatch: Boolean
  ): Future[Seq[GoodsItemRecord]] =
    searchTerm match {
      case Some(value) =>
        field match {
          case Some(searchField) =>
            collection
              .find[GoodsItemRecord](
                Filters.and(
                  byEoriAndActive(eori),
                  byField(searchField, value, exactMatch)
                )
              )
              .sort(byLatest)
              .toFuture()
          case _                 =>
            collection
              .find[GoodsItemRecord](
                Filters.and(
                  byEoriAndActive(eori),
                  byComCodeOrGoodsDescriptionOrTraderRef(value, exactMatch)
                )
              )
              .sort(byLatest)
              .toFuture()
        }
      case None        => Future.successful(Seq.empty)
    }

  def deleteManyByEori(eori: String): Future[Long] =
    collection.deleteMany(byEori(eori)).toFuture().map(result => result.getDeletedCount)

  def deleteManyByEoriAndInactive(eori: String): Future[Long] =
    collection.deleteMany(byEoriAndInactive(eori)).toFuture().map(result => result.getDeletedCount)
}
