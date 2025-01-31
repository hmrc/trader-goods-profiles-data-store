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
import org.mongodb.scala.gridfs.ObservableFuture
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadySeen, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataSummary

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DownloadDataSummaryRepository @Inject() (
  mongoComponent: MongoComponent,
  config: DataStoreAppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[DownloadDataSummary](
      collectionName = "downloadDataSummary",
      mongoComponent = mongoComponent,
      domainFormat = DownloadDataSummary.mongoFormat,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("expiresAt"),
          IndexOptions()
            .name("expiresAtIndex")
            .expireAfter(0, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.ascending("eori"),
          IndexOptions()
            .name("eori_idx")
        )
      ),
      replaceIndexes = config.downloadDataSummaryReplaceIndexes
    ) {

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  private def byEoriAndSummaryId(eori: String, summaryId: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.and(Filters.equal("summaryId", summaryId)))

  private def byOldest: Bson = Sorts.ascending("createdAt")

  private def byEoriAndFileInProgress(eori: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("status", FileInProgress.toString))

  private def byEoriAndFileReadyUnseen(eori: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("status", FileReadyUnseen.toString))

  def get(eori: String): Future[Seq[DownloadDataSummary]] = Mdc.preservingMdc {
    collection
      .find[DownloadDataSummary](byEori(eori))
      .toFuture()
  }

  def get(eori: String, summaryId: String): Future[Option[DownloadDataSummary]] = Mdc.preservingMdc {
    if (config.useXConversationIdHeader) {
      collection
        .find[DownloadDataSummary](byEoriAndSummaryId(eori, summaryId))
        .headOption()
    } else {
      collection
        .find[DownloadDataSummary](byEoriAndFileInProgress(eori))
        .sort(byOldest)
        .headOption()
    }
  }

  def updateSeen(eori: String): Future[Long] = Mdc.preservingMdc {
    collection
      .updateMany(
        filter = byEoriAndFileReadyUnseen(eori),
        update = Updates.set("status", FileReadySeen.toString)
      )
      .head()
      .map(_.getMatchedCount)
  }

  def set(downloadDataSummary: DownloadDataSummary): Future[Done] = Mdc.preservingMdc {
    collection
      .replaceOne(
        filter = byEoriAndSummaryId(downloadDataSummary.eori, downloadDataSummary.summaryId),
        replacement = downloadDataSummary,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => Done)
  }
}
