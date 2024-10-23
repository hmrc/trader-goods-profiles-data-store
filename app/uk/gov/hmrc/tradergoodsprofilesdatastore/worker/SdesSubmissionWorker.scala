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
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.SdesService
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
class SdesSubmissionWorker @Inject() (
  actorSystem: ActorSystem,
  configuration: Configuration,
  sdesService: SdesService,
  applicationLifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext)
    extends Logging {

  private val initialDelay = configuration.get[FiniteDuration]("workers.sdes-submission.initial-delay")
  private val interval     = configuration.get[FiniteDuration]("workers.sdes-submission.interval")

  private val scheduler = actorSystem.scheduler

  private val cancellable = scheduler.scheduleAtFixedRate(initialDelay, interval) { () =>
    logger.info("Starting to process pending SDES submissions")
    sdesService.processAllSubmissions().onComplete {
      case Success(_)           =>
        logger.info("Completed processing pending SDES submissions")
      case Failure(NonFatal(e)) =>
        logger.error("Error while processing pending SDES submissions", e)
      case Failure(e)           =>
        throw e
    }
  }

  applicationLifecycle.addStopHook(() => Future.successful(cancellable.cancel()))
}
