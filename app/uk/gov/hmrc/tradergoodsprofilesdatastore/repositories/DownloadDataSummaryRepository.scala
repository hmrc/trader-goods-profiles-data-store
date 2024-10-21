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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.FileInProgress
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataSummary

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
          Indexes.compoundIndex(
            Indexes.ascending("eori"),
            Indexes.ascending("_id")
          ),
          IndexOptions()
            .name("eori_summaryId_idx")
        )
      )
    ) {

  //TODO TTL

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  private def byEoriAndSummaryIds(eori: String, summaryIds: Seq[String]): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.in("summaryId", summaryIds: _*))

  private def byEoriAndSummaryId(eori: String, summaryId: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.and(Filters.equal("summaryId", summaryId)))

  private def byLatest: Bson = Sorts.descending("createdAt")

  private def byEoriAndFileInProgress(eori: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("status", FileInProgress.toString))

  def get(eori: String): Future[Seq[DownloadDataSummary]] = Mdc.preservingMdc {
    collection
      .find[DownloadDataSummary](byEori(eori))
      .toFuture()
  }

  //TODO matching on an ID
  def getLatestInProgress(eori: String): Future[Option[DownloadDataSummary]] = Mdc.preservingMdc {
    collection
      .find[DownloadDataSummary](byEoriAndFileInProgress(eori))
      .sort(byLatest)
      .headOption()
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

  //TODO will go as not needed due to TTL
  def deleteMany(eori: String, summaryIds: Seq[String]): Future[Long] = Mdc.preservingMdc {
    collection
      .deleteMany(filter = byEoriAndSummaryIds(eori, summaryIds))
      .toFuture()
      .map(_.getDeletedCount)
  }
}
