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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.ClearCacheService

import java.time.Instant
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

class ClearCacheWorkerSpec
    extends AnyFreeSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val mockAppConfig         = mock[DataStoreAppConfig]
  private val mockLifecycle         = mock[ApplicationLifecycle]
  private val mockClearCacheService = mock[ClearCacheService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig, mockLifecycle, mockClearCacheService)
  }

  private def withActorSystem(testCode: ActorSystem => Any): Unit = {
    val system = ActorSystem("test")
    try testCode(system)
    finally {
      system.terminate()
      Thread.sleep(200)
    }
  }

  "ClearCacheWorker" - {
    "schedule cache clearing when enabled" in withActorSystem { actorSystem =>
      when(mockAppConfig.clearCacheWorkerEnabled).thenReturn(true)
      when(mockAppConfig.clearCacheWorkerInterval).thenReturn(1.hour)
      when(mockAppConfig.clearCacheWorkerInitialDelay).thenReturn(0.seconds)
      when(mockAppConfig.dataToClearOlderThanDays).thenReturn(30.days)
      when(mockClearCacheService.clearCache(any[Instant])).thenReturn(Future.successful(()))

      new ClearCacheWorker(mockAppConfig, mockLifecycle, actorSystem, mockClearCacheService)

      Thread.sleep(500)
      verify(mockClearCacheService).clearCache(any[Instant])
    }

    "not schedule cache clearing when disabled" in withActorSystem { actorSystem =>
      when(mockAppConfig.clearCacheWorkerEnabled).thenReturn(false)

      new ClearCacheWorker(mockAppConfig, mockLifecycle, actorSystem, mockClearCacheService)

      verify(mockClearCacheService, never()).clearCache(any[Instant])
    }

    "log errors when cache clearing fails" in withActorSystem { actorSystem =>
      when(mockAppConfig.clearCacheWorkerEnabled).thenReturn(true)
      when(mockAppConfig.clearCacheWorkerInterval).thenReturn(1.hour)
      when(mockAppConfig.clearCacheWorkerInitialDelay).thenReturn(0.seconds)
      when(mockAppConfig.dataToClearOlderThanDays).thenReturn(30.days)
      when(mockClearCacheService.clearCache(any[Instant]))
        .thenReturn(Future.failed(new RuntimeException("Test failure")))

      new ClearCacheWorker(mockAppConfig, mockLifecycle, actorSystem, mockClearCacheService)

      Thread.sleep(500)
      verify(mockClearCacheService).clearCache(any[Instant])
    }
  }
}
