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
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsSummaryRepository.NoRecordFound

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RecordsSummaryRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[RecordsSummary](
      collectionName = "recordsSummary",
      mongoComponent = mongoComponent,
      domainFormat = RecordsSummary.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("eori"),
          IndexOptions()
            .name("eori_idx")
            .unique(true)
        ),
        IndexModel(
          Indexes.descending("lastUpdated"),
          IndexOptions()
            .name("lastUpdated_idx")
        )
      ),
      extraCodecs = Seq(Codecs.playFormatCodec(RecordsSummary.Update.format))
    ) {

  // We will be handling the timing out of this data with a worker
  // rather than a TTL as this will need to be coordinated
  override lazy val requiresTtlIndex: Boolean = false

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  def get(eori: String): Future[Option[RecordsSummary]] = Mdc.preservingMdc {
    collection
      .find[RecordsSummary](byEori(eori))
      .headOption()
  }

  def set(eori: String, update: Option[Update], lastUpdated: Instant): Future[Done] = Mdc.preservingMdc {
    val recordsSummary = RecordsSummary(eori, update, lastUpdated)
    collection
      .replaceOne(
        filter = byEori(eori),
        replacement = recordsSummary,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => Done)
  }

  def update(eori: String, update: Option[Update], lastUpdated: Option[Instant]): Future[Done] = Mdc.preservingMdc {

    val updates = Seq(
      Some(update.map(Updates.set("currentUpdate", _)).getOrElse(Updates.unset("currentUpdate"))),
      lastUpdated.map(Updates.set("lastUpdated", _))
    ).flatten

    collection
      .updateOne(
        filter = byEori(eori),
        update = Updates.combine(updates: _*)
      )
      .head()
      .flatMap { result =>
        if (result.getMatchedCount > 0) {
          Future.successful(Done)
        } else {
          Future.failed(NoRecordFound(eori))
        }
      }
  }
}

object RecordsSummaryRepository {

  final case class NoRecordFound(eori: String) extends Throwable {
    override def getMessage: String = s"No summary record found for $eori"
  }
}
