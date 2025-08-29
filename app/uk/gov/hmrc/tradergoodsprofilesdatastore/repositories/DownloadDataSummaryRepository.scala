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
import org.mongodb.scala.model.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileFailedSeen, FileFailedUnseen, FileInProgress, FileReadySeen, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataSummary

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.ObservableFuture
import uk.gov.hmrc.mongo.play.json.Codecs.logger

import java.time.{Clock, Instant}
import java.time.temporal.ChronoUnit

@Singleton
class DownloadDataSummaryRepository @Inject() (
  mongoComponent: MongoComponent,
  config: DataStoreAppConfig,
  clock: Clock
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

  private def byEoriAndFileFailedUnseen(eori: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("status", FileFailedUnseen.toString))

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
    for {
      readyCount  <- collection
                       .updateMany(
                         filter = byEoriAndFileReadyUnseen(eori),
                         update = Updates.combine(
                           Updates.set("status", FileReadySeen.toString),
                           Updates.set("updatedAt", Instant.now())
                         )
                       )
                       .head()
                       .map(_.getMatchedCount)
      failedCount <- collection
                       .updateMany(
                         filter = byEoriAndFileFailedUnseen(eori),
                         update = Updates.combine(
                           Updates.set("status", FileFailedSeen.toString),
                           Updates.set("updatedAt", Instant.now())
                         )
                       )
                       .head()
                       .map(_.getMatchedCount)
    } yield readyCount + failedCount
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

  def markAsFailed(eori: String): Future[Long] = Mdc.preservingMdc {
    val slaThreshold = clock.instant().minus(24, ChronoUnit.HOURS)
    collection
      .updateMany(
        filter = Filters.and(
          Filters.eq("eori", eori),
          Filters.eq("status", FileInProgress.toString),
          Filters.lt("createdAt", slaThreshold)
        ),
        update = Updates.combine(
          Updates.set("status", FileFailedUnseen.toString),
          Updates.set("updatedAt", clock.instant())
        )
      )
      .toFuture()
      .map(_.getModifiedCount)
      .recover { case e =>
        logger.error(
          s"[DownloadDataSummaryRepository] - Error marking summaries as failed for EORI $eori: ${e.getMessage}"
        )
        throw e
      }
  }
}
