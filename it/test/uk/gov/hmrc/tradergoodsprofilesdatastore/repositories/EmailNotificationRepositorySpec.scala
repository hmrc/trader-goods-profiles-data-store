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
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{EmailNotification, NotificationData}

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class EmailNotificationRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with PlayMongoRepositorySupport[EmailNotification]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareDatabase()
  }

  private val testRecordId = "test-recordId"

  private val notification =
    EmailNotification(
      "test-eori",
      testRecordId,
      NotificationData(Some("test-expired-date")),
      Instant.parse("2024-09-05T16:12:34Z")
    )

  protected override val repository = new EmailNotificationsRepository(mongoComponent = mongoComponent)

  ".create" - {

    "must create a record" in {
      val setResult     = repository.create(notification).futureValue
      val updatedRecord = repository.getMany(testRecordId).futureValue

      setResult mustEqual Done
      updatedRecord mustEqual Seq(notification)
    }
  }

  ".getMany" - {

    "when there are notification for this recordId it must get the records" in {
      insert(notification).futureValue
      val result = repository.getMany(testRecordId).futureValue
      result mustEqual Seq(notification)
    }

    "when there are not notifications for this recordId it must not return no records" in {
      val result = repository.getMany(testRecordId).futureValue
      result mustEqual Seq()
    }
  }

  ".deleteAll" - {
    "it mush drop the notification collection" in {
      insert(notification).futureValue
      val result      = repository.deleteAll().futureValue
      val recordCheck = repository.getMany(testRecordId).futureValue
      result mustEqual Done
      recordCheck mustEqual Seq.empty
    }
  }
}
