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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update

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
          IndexOptions().name("eori")
        )
      )
    ) {

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  def get(eori: String): Future[Option[RecordsSummary]] =
    collection
      .find[RecordsSummary](byEori(eori))
      .headOption()

  def set(eori: String, update: Option[Update]): Future[Boolean] = {
    val recordsSummary = RecordsSummary(eori, update, Instant.now)
    collection
      .replaceOne(
        filter = byEori(recordsSummary.eori),
        replacement = recordsSummary,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }
}
