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

import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Configuration
import uk.gov.hmrc.mongo.workitem.{WorkItemFields, WorkItemRepository}
import uk.gov.hmrc.mongo.{MongoComponent, MongoUtils}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataSummary

import java.time.{Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SdesSubmissionWorkItemRepository @Inject() (
  configuration: Configuration,
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends WorkItemRepository[DownloadDataSummary](
      collectionName = "sdesSubmissions",
      mongoComponent = mongoComponent,
      itemFormat = DownloadDataSummary.mongoFormat,
      workItemFields = WorkItemFields.default
    ) {

  override def now(): Instant =
    Instant.now()

  override val inProgressRetryAfter: Duration =
    configuration.get[Duration]("sdes.submission.retry-after")

  override def ensureIndexes(): Future[Seq[String]] = {
    val workItemIndexes: Seq[IndexModel] =
      indexes ++ List(IndexModel(ascending("item.summaryId"), IndexOptions().name("summaryIdIdx").unique(true)))
    MongoUtils.ensureIndexes(collection, workItemIndexes, replaceIndexes = true)
  }

}
