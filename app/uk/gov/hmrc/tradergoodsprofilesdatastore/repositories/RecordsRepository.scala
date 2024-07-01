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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GoodsItemRecords

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecordsRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[GoodsItemRecords](
      collectionName = "goodsItemRecords",
      mongoComponent = mongoComponent,
      domainFormat = GoodsItemRecords.goodsItemRecordsFormat,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("recordId"),
          IndexOptions().name("recordId")
        )
      )
    ) {

  private def byRecordId(recordId: String): Bson = Filters.equal("recordId", recordId)
  private def byEori(eori: String): Bson         = Filters.equal("eori", eori)
  private def byLatest: Bson                     = Sorts.descending("updatedDateTime")

  def saveRecords(records: Seq[GoodsItemRecords]): Future[Boolean] =
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

  def get(recordId: String): Future[Option[GoodsItemRecords]] =
    collection
      .find[GoodsItemRecords](byRecordId(recordId))
      .headOption()

  def getLatest(eori: String): Future[Option[GoodsItemRecords]] =
    collection
      .find[GoodsItemRecords](byEori(eori))
      .limit(1)
      .sort(byLatest)
      .headOption()

  def delete(recordId: String): Future[Boolean] =
    collection
      .deleteOne(
        filter = byRecordId(recordId)
      )
      .toFuture()
      .map(_.getDeletedCount > 0)
}
