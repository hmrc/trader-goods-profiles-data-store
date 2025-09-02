/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.Logging
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.DownloadFailureService
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class StaleDownloadWorker @Inject() (
  actorSystem: ActorSystem,
  configuration: Configuration,
  downloadFailureService: DownloadFailureService,
  applicationLifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext)
    extends Logging {

  private val initialDelay = configuration.get[FiniteDuration]("workers.stale-download-worker.initial-delay")
  private val interval     = configuration.get[FiniteDuration]("workers.stale-download-worker.interval")
  private val enabled      = configuration.get[Boolean]("workers.stale-download-worker.enabled")

  private val scheduler = actorSystem.scheduler

  if (enabled) {
    logger.info("[StaleDownloadWorker] - Starting StaleDownloadWorker")
    val cancellable = scheduler.scheduleWithFixedDelay(initialDelay, interval) { () =>
      logger.info("[StaleDownloadWorker] - Checking stale downloads")
      downloadFailureService.processStaleDownloads().onComplete {
        case Success(_) => logger.info("[StaleDownloadWorker] - Completed processing stale downloads")
        case Failure(e) => logger.error("[StaleDownloadWorker] - Error processing stale downloads", e)
      }
    }
    applicationLifecycle.addStopHook { () =>
      logger.info("[StaleDownloadWorker] - Stopping StaleDownloadWorker")
      Future.successful(cancellable.cancel())
    }
  } else {
    logger.info("[StaleDownloadWorker] - StaleDownloadWorker disabled")
  }
}
