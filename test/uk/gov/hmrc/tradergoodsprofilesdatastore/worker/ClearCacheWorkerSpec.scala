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
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.ClearCacheService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DAYS, FiniteDuration, MILLISECONDS}

class ClearCacheWorkerSpec extends PlaySpec with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val configuration        = mock[Configuration]
  private val applicationLifecycle = mock[ApplicationLifecycle]
  private val actorSystem          = mock[ActorSystem]
  private val clearCacheService    = mock[ClearCacheService]

  private val sut = new ClearCacheWorker(configuration, applicationLifecycle, actorSystem, clearCacheService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(configuration, applicationLifecycle, actorSystem, clearCacheService)

    when(configuration.get[FiniteDuration]("workers.clear-cache-worker.interval"))
      .thenReturn(new FiniteDuration(1, MILLISECONDS))
    when(configuration.get[FiniteDuration]("workers.clear-cache-worker.initial-delay"))
      .thenReturn(new FiniteDuration(1, MILLISECONDS))
    when(configuration.get[FiniteDuration]("workers.clear-cache-worker.older-than-days"))
      .thenReturn(new FiniteDuration(180, DAYS))
  }

  "clearCacheWorker" should {

    "run the scheduler and clear cache when enabled" in {}

    "not run the scheduler if it is disabled" in {
      when(configuration.get[Boolean]("workers.clear-cache-worker.enabled"))
        .thenReturn(false)

    }

  }
}
