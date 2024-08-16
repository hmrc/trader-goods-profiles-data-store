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

import play.api.Logging
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClearCacheService @Inject() (
  recordsSummaryRepository: RecordsSummaryRepository,
  recordsRepository: RecordsRepository,
  mongoLockRepository: MongoLockRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  private val lockService = LockService(mongoLockRepository, lockId = "clear-cache-lock", ttl = 1.hour)

  def clearCache(lastUpdatedBefore: Instant) =
    lockService
      .withLock {
        recordsSummaryRepository.getByLastUpdatedBefore(lastUpdatedBefore).flatMap { recordSummaries =>
          recordSummaries.map { recordSummary =>
            recordsRepository.deleteRecordsByEori(recordSummary.eori)
            recordsSummaryRepository.deleteByEori(recordSummary.eori)
          }
          Future.successful(None)
        }
      }
      .map {
        case Some(res) => logger.debug(s"Finished with $res. Lock has been released.")
        case None      => logger.debug("Failed to take lock")
      }
}
