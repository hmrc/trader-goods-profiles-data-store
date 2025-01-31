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

import org.apache.pekko.actor.{ActorSystem, Cancellable, Scheduler}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.ClearCacheService

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

class ClearCacheWorkerSpec extends AnyWordSpecLike with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val appConfig            = mock[DataStoreAppConfig]
  private val applicationLifecycle = mock[ApplicationLifecycle]
  private val actorSystem          = mock[ActorSystem]
  private val clearCacheService    = mock[ClearCacheService]
  private val scheduler            = mock[Scheduler]

  private object TestJob extends Cancellable {
    def cancel(): Boolean    = false
    def isCancelled: Boolean = false
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(actorSystem, scheduler, applicationLifecycle)
    when(actorSystem.scheduler).thenReturn(scheduler)
    when(scheduler.scheduleWithFixedDelay(any, any)(any)(any)).thenReturn(TestJob)
    when(appConfig.clearCacheWorkerInterval).thenReturn(100.milliseconds)
    when(appConfig.clearCacheWorkerInitialDelay).thenReturn(200.milliseconds)
  }

  "clearCacheWorker" should {

    "run the scheduler and clear cache when enabled" in {
      when(appConfig.clearCacheWorkerEnabled).thenReturn(true)

      new ClearCacheWorker(appConfig, applicationLifecycle, actorSystem, clearCacheService)

      verify(scheduler).scheduleWithFixedDelay(eqTo(200.milliseconds), eqTo(100.milliseconds))(any)(
        any[ExecutionContext]
      )

      verify(applicationLifecycle).addStopHook(any[() => Future[_]])
    }

    "not run the scheduler if it is disabled" in {
      when(appConfig.clearCacheWorkerEnabled).thenReturn(false)

      new ClearCacheWorker(appConfig, applicationLifecycle, actorSystem, clearCacheService)

      verifyNoInteractions(scheduler)
      verifyNoInteractions(applicationLifecycle)
    }

  }
}
