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

package uk.gov.hmrc.tradergoodsprofilesdatastore.worker

import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.ClearCacheService

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ClearCacheWorker @Inject() (
  appConfig: DataStoreAppConfig,
  lifecycle: ApplicationLifecycle,
  actorSystem: ActorSystem,
  clearCacheService: ClearCacheService
)(implicit ec: ExecutionContext)
    extends Logging {

  private val scheduler = actorSystem.scheduler

  private val interval                 = appConfig.clearCacheWorkerInterval
  private val initialDelay             = appConfig.clearCacheWorkerInitialDelay
  private val enabled                  = appConfig.clearCacheWorkerEnabled
  private val dataToClearOlderThanDays = appConfig.dataToClearOlderThanDays

  if (enabled) {

    logger.info("[ClearCacheWorker] - Starting ClearCacheWorker")
    val cancel = scheduler.scheduleWithFixedDelay(initialDelay, interval) { () =>
      clearCacheService
        .clearCache(Instant.now().minus(dataToClearOlderThanDays.toDays, ChronoUnit.DAYS))
        .onComplete {
          case Success(_) => ()
          case Failure(e) =>
            logger.error("[ClearCacheWorker] - Error while clearing cache", e)
        }
    }
    logger.info("[ClearCacheWorker] - Stopped ClearCacheWorker")
    lifecycle.addStopHook(() => Future.successful(cancel.cancel()))
  } else {
    logger.info("[ClearCacheWorker] - ClearCacheWorker disabled")
  }
}
