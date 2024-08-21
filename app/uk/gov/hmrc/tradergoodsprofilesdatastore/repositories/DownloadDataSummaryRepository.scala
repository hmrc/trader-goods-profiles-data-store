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

import org.apache.pekko.Done
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataStatus, DownloadDataSummary}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DownloadDataSummaryRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[DownloadDataSummary](
      collectionName = "downloadDataSummary",
      mongoComponent = mongoComponent,
      domainFormat = DownloadDataSummary.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("eori"),
          IndexOptions()
            .name("eori_idx")
            .unique(true)
        )
      )
    ) {

  private def byEori(eori: String): Bson      = Filters.equal("eori", eori)
  override lazy val requiresTtlIndex: Boolean = false

  def get(eori: String): Future[Option[DownloadDataSummary]] = Mdc.preservingMdc {
    collection
      .find[DownloadDataSummary](byEori(eori))
      .headOption()
  }

  def set(downloadDataSummary: DownloadDataSummary): Future[Done] = Mdc.preservingMdc {
    collection
      .replaceOne(
        filter = byEori(downloadDataSummary.eori),
        replacement = downloadDataSummary,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => Done)
  }

  def update(eori: String, status: DownloadDataStatus): Future[Boolean] = Mdc.preservingMdc {
    val updates = Seq(
      Updates.set("status", status.toString)
    )
    collection
      .updateOne(
        filter = byEori(eori),
        update = Updates.combine(updates: _*)
      )
      .toFuture()
      .map(result => result.getMatchedCount > 0)
  }
}
