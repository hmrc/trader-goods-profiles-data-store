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
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logging}
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.ClearCacheService

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ClearCacheWorker @Inject() (
  configuration: Configuration,
  lifecycle: ApplicationLifecycle,
  actorSystem: ActorSystem,
  clearCacheService: ClearCacheService
)(implicit ec: ExecutionContext)
    extends Logging {

  private val scheduler = actorSystem.scheduler

  private val interval                 = configuration.get[FiniteDuration]("workers.clear-cache-worker.interval")
  private val initialDelay             = configuration.get[FiniteDuration]("workers.clear-cache-worker.initial-delay")
  private val enabled                  = configuration.get[Boolean]("workers.clear-cache-worker.enabled")
  private val dataToClearOlderThanDays = configuration.get[FiniteDuration]("workers.clear-cache-worker.older-than-days")

  if (enabled) {

    logger.info("Starting ClearCacheWorker")
    val cancel = scheduler.scheduleWithFixedDelay(initialDelay, interval) { () =>
      clearCacheService.clearCache(Instant.now().minus(dataToClearOlderThanDays.toDays, ChronoUnit.DAYS)).onComplete {
        case Success(_) => ()
        case Failure(e) => logger.error("Error while clearing cache", e)
      }
    }
    logger.info("Stopped ClearCacheWorker")
    lifecycle.addStopHook(() => Future.successful(cancel.cancel()))
  } else {
    logger.info("ClearCacheWorker disabled")
  }
}
