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
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GoodsItemRecord
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{localPageSize, localStartingPage}
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.RepositoryHelpers.caseInsensitiveCollation
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.StringHelper.escapeRegexSpecialChars

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class RecordsRepository @Inject() (
  override val mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[GoodsItemRecord](
      collectionName = "goodsItemRecords",
      mongoComponent = mongoComponent,
      domainFormat = GoodsItemRecord.goodsItemRecordsMongoFormat,
      indexes = Seq(
        IndexModel(
          Indexes.compoundIndex(
            Indexes.ascending("eori"),
            Indexes.ascending("_id")
          ),
          IndexOptions()
            .name("eori_recordId_idx")
        ),
        IndexModel(
          Indexes.ascending("traderRef"),
          IndexOptions().collation(
            caseInsensitiveCollation
          )
        )
      )
    )
    with Transactions {

  // We will be handling the timing out of this data with a worker
  // rather than a TTL as this will need to be coordinated
  override lazy val requiresTtlIndex: Boolean = false

  private implicit val tc: TransactionConfiguration =
    TransactionConfiguration.strict

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  private def byEoriAndRecordId(eori: String, recordId: String): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.equal("_id", recordId))

  private def byEoriAndRecordIds(eori: String, recordIds: Seq[String]): Bson =
    Filters.and(Filters.equal("eori", eori), Filters.in("_id", recordIds *))

  private def byLatest: Bson = Sorts.descending("updatedDateTime")

  private def byField(field: String, searchTerm: String, exactMatch: Boolean): Bson =
    if (exactMatch) {
      Filters.equal(field, searchTerm)
    } else {
      // TODO we must not allow people to give us regexes to execute
      Filters.regex(field, searchTerm, "i")
    }

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
      // TODO we must not allow people to give us regexes to execute
      Filters.or(
        Filters.regex("traderRef", searchPattern, "i"),
        Filters.regex("goodsDescription", searchPattern, "i"),
        Filters.regex("comcode", searchPattern, "i")
      )
    }

  def updateRecords(eori: String, records: Seq[GoodsItemRecord]): Future[Done] = Mdc.preservingMdc {
    val (activeRecords, inactiveRecords) = records.partition(_.active)
    val inactiveRecordIds                = inactiveRecords.map(_.recordId)
    withSessionAndTransaction { session =>
      collection
        .bulkWrite(
          session,
          activeRecords.map { record =>
            ReplaceOneModel(
              filter = byEoriAndRecordId(eori, record.recordId),
              replacement = record,
              replaceOptions = ReplaceOptions().upsert(true)
            )
          } :+ DeleteManyModel(byEoriAndRecordIds(eori, inactiveRecordIds))
        )
        .toFuture()
        .map(_ => Done)
    }
  }

  def getMany(eori: String, pageOpt: Option[Int], sizeOpt: Option[Int]): Future[Seq[GoodsItemRecord]] =
    Mdc.preservingMdc {
      val size = sizeOpt.getOrElse(localPageSize)
      val page = pageOpt.getOrElse(localStartingPage)
      val skip = (page - 1) * size
      collection
        .find[GoodsItemRecord](byEori(eori))
        .sort(byLatest)
        .limit(size)
        .skip(skip)
        .toFuture()
    }

  def getCount(eori: String): Future[Long] = Mdc.preservingMdc {
    collection
      .countDocuments(byEori(eori))
      .toFuture()
  }

  def deleteRecordsByEori(eori: String): Future[Long] = Mdc.preservingMdc {
    collection
      .deleteMany(byEori(eori))
      .toFuture()
      .map(_.getDeletedCount)
  }

  // TODO need to add an appropriate index for this to search
  def filterRecords(
    eori: String,
    searchTerm: Option[String],
    field: Option[String],
    exactMatch: Boolean
  ): Future[Seq[GoodsItemRecord]] = Mdc.preservingMdc {
    searchTerm match {
      case Some(value) =>
        field match {

          case Some("traderRef") =>
            collection
              .find[GoodsItemRecord](
                Filters.and(
                  byEori(eori),
                  byField("traderRef", value, exactMatch)
                )
              )
              .collation(
                caseInsensitiveCollation
              )
              .sort(byLatest)
              .toFuture()

          case Some(searchField) =>
            collection
              .find[GoodsItemRecord](
                Filters.and(
                  byEori(eori),
                  byField(searchField, value, exactMatch)
                )
              )
              .sort(byLatest)
              .toFuture()
          case _                 =>
            collection
              .find[GoodsItemRecord](
                Filters.and(
                  byEori(eori),
                  byComCodeOrGoodsDescriptionOrTraderRef(value, exactMatch)
                )
              )
              .sort(byLatest)
              .toFuture()
        }
      case None        => Future.successful(Seq.empty)
    }
  }

  def searchTermFilter(searchTerm: Option[String]): Bson =
    searchTerm match {
      case Some(value) =>
        val escapedValue = escapeRegexSpecialChars(value)
        Filters.or(
          Filters.regex("traderRef", escapedValue, "i"),
          Filters.regex("goodsDescription", escapedValue, "i"),
          Filters.regex("comcode", escapedValue, "i")
        )
      case None        => Filters.exists("traderRef")
    }

  def countryOfOriginFilter(countryOfOrigin: Option[String]): Bson =
    countryOfOrigin match {
      case Some(value) => Filters.equal("countryOfOrigin", value)
      case None        => Filters.exists("countryOfOrigin")
    }

  def declarableFilter(
    IMMIReady: Option[Boolean],
    notReadyForIMMI: Option[Boolean],
    actionNeeded: Option[Boolean]
  ): Bson = {
    val conditions = List(
      if (IMMIReady.contains(true)) Some(Filters.equal("declarable", "IMMI Ready")) else None,
      if (notReadyForIMMI.contains(true)) Some(Filters.equal("declarable", "Not ready for IMMI")) else None,
      if (actionNeeded.contains(true)) Some(Filters.equal("declarable", "Not Ready For Use")) else None
    ).flatten

    if (conditions.isEmpty) {
      Filters.exists("declarable")
    } else {
      Filters.or(conditions *)
    }
  }

  def filterRecordsIteration(
    eori: String,
    searchTerm: Option[String],
    countryOfOrigin: Option[String],
    IMMIReady: Option[Boolean],
    notReadyForIMMI: Option[Boolean],
    actionNeeded: Option[Boolean]
  ): Future[Seq[GoodsItemRecord]] = Mdc.preservingMdc {
    collection
      .find[GoodsItemRecord](
        Filters.and(
          byEori(eori),
          searchTermFilter(searchTerm),
          countryOfOriginFilter(countryOfOrigin),
          declarableFilter(IMMIReady, notReadyForIMMI, actionNeeded)
        )
      )
      .sort(byLatest)
      .toFuture()
  }

  def isTraderReferenceUnique(eori: String, traderReference: String): Future[Boolean] = Mdc.preservingMdc {
    val caseInsensitiveFilter: Bson = Filters.regex("traderRef", s"^${Regex.quote(traderReference)}$$", "i")

    collection
      .countDocuments(
        Filters.and(
          byEori(eori),
          caseInsensitiveFilter
        )
      )
      .toFuture()
      .map(_ == 0)
  }
}
