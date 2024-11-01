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

package uk.gov.hmrc.tradergoodsprofilesdatastore.services

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.{never, times}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.TestConstants.testEori
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class ClearCacheServiceSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val recordsSummaryRepository = mock[RecordsSummaryRepository]
  private val recordsRepository        = mock[RecordsRepository]
  private val mongoLockRepository      = mock[MongoLockRepository]

  private val sut = new ClearCacheService(
    recordsSummaryRepository,
    recordsRepository,
    mongoLockRepository
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(recordsSummaryRepository, recordsRepository, mongoLockRepository)

    when(mongoLockRepository.takeLock(any, any, any))
      .thenReturn(Future.successful(Some(Lock("clear-cache-lock", "123", Instant.now, Instant.now))))
    when(mongoLockRepository.releaseLock(any, any)).thenReturn(Future.successful(Done))
  }

  "clearCache" should {
    "acquire a lock" in {
      when(recordsSummaryRepository.getByLastUpdatedBefore(any))
        .thenReturn(Future.successful(Seq()))

      await(sut.clearCache(Instant.now))

      verify(mongoLockRepository, times(1)).takeLock(eqTo("clear-cache-lock"), any, any)
    }

    "release a lock" in {
      when(recordsSummaryRepository.getByLastUpdatedBefore(any))
        .thenReturn(Future.successful(Seq()))

      await(sut.clearCache(Instant.now))

      verify(mongoLockRepository, times(1)).releaseLock(eqTo("clear-cache-lock"), any)
    }

    "delete a cache" in {
      val sampleRecordsSummary: RecordsSummary = RecordsSummary(
        eori = testEori,
        currentUpdate = Some(Update(0, 0)),
        lastUpdated = Instant.now().minus(182, ChronoUnit.DAYS)
      )
      when(recordsSummaryRepository.getByLastUpdatedBefore(any))
        .thenReturn(Future.successful(Seq(sampleRecordsSummary)))
      when(recordsRepository.deleteRecordsByEori(any)).thenReturn(Future.successful(1))
      when(recordsSummaryRepository.deleteByEori(any)).thenReturn(Future.successful(1))

      val expiredBefore = Instant.now
      await(sut.clearCache(expiredBefore))

      verify(recordsSummaryRepository, times(1)).getByLastUpdatedBefore(eqTo(expiredBefore))
      verify(recordsSummaryRepository, times(1)).deleteByEori(eqTo(testEori))
      verify(recordsRepository, times(1)).deleteRecordsByEori(eqTo(testEori))
    }

    "should not delete cache if record is before expired date" in {
      val expiredBefore = Instant.now
      when(recordsSummaryRepository.getByLastUpdatedBefore(any))
        .thenReturn(Future.successful(Seq()))

      await(sut.clearCache(expiredBefore))

      verify(recordsSummaryRepository, times(1)).getByLastUpdatedBefore(eqTo(expiredBefore))
      verify(recordsSummaryRepository, never()).deleteByEori(any)
      verifyZeroInteractions(recordsRepository)
    }

    "should not delete cache if recordsRepository fails to delete records" in {
      val sampleRecordsSummary: RecordsSummary = RecordsSummary(
        eori = testEori,
        currentUpdate = Some(Update(0, 0)),
        lastUpdated = Instant.now().minus(182, ChronoUnit.DAYS)
      )
      when(recordsSummaryRepository.getByLastUpdatedBefore(any))
        .thenReturn(Future.successful(Seq(sampleRecordsSummary)))
      when(recordsRepository.deleteRecordsByEori(any)).thenThrow(new RuntimeException("error"))

      val expiredBefore = Instant.now
      intercept[RuntimeException] {
        await(sut.clearCache(expiredBefore))
      }

      verify(recordsSummaryRepository, never).deleteByEori(any)
    }
  }
}
