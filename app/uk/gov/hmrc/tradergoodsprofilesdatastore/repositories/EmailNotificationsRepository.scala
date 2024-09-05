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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.EmailNotification
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.EmailNotificationRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailNotificationsRepository @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[EmailNotification](
      collectionName = "emailNotifications",
      mongoComponent = mongoComponent,
      domainFormat = EmailNotification.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("recordId"),
          IndexOptions().name("recordId")
        )
      )
    ) {

  private def byRecordId(recordId: String): Bson = Filters.equal("recordId", recordId)

  def create(eori: String, recordId: String, notificationRequest: EmailNotificationRequest): Future[Done] = {
    val notificationToBeCreated = EmailNotification.fromRequest(eori, recordId, notificationRequest)
    collection
      .insertOne(notificationToBeCreated)
      .toFuture()
      .map(_ => Done)
  }

  def getMany(record: String): Future[Seq[EmailNotification]] =
    collection
      .find[EmailNotification](byRecordId(record))
      .toFuture()

  def deleteAll(): Future[Done] =
    collection.drop().toFuture().map(_ => Done)
}
