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

import uk.gov.hmrc.tradergoodsprofilesdatastore.models.Profile
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProfileRepository @Inject() (
                                    mongoComponent: MongoComponent,
                                  )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[Profile](
    collectionName = "profiles",
    mongoComponent = mongoComponent,
    domainFormat = Profile.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("eori"),
        IndexOptions().name("eori")
      )
    )
  ) {

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  def get(eori: String): Future[Option[Profile]] =
      collection
        .find[Profile](byEori(eori))
        .headOption()

  def set(profile: Profile): Future[Boolean] =
    collection
      .replaceOne(
        filter = byEori(profile.eori),
        replacement = profile,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)

}
