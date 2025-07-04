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

package uk.gov.hmrc.tradergoodsprofilesdatastore.config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.EnrolmentConfig

import java.time.Duration
import scala.concurrent.duration.FiniteDuration
@Singleton
class DataStoreAppConfig @Inject() (configuration: Configuration) {
  val tgpEnrolmentIdentifier: EnrolmentConfig      = configuration.get[EnrolmentConfig]("enrolment-config")
  val checkForHistoricProfile: Boolean             = configuration.get[Boolean]("features.check-for-historic-profile")
  val sendNotificationEmail: Boolean               = configuration.get[Boolean]("features.send-notification-email")
  val clearCacheWorkerEnabled: Boolean             = configuration.get[Boolean]("workers.clear-cache-worker.enabled")
  val clearCacheWorkerInterval: FiniteDuration     =
    configuration.get[FiniteDuration]("workers.clear-cache-worker.interval")
  val clearCacheWorkerInitialDelay: FiniteDuration =
    configuration.get[FiniteDuration]("workers.clear-cache-worker.initial-delay")
  val dataToClearOlderThanDays: FiniteDuration     =
    configuration.get[FiniteDuration]("workers.clear-cache-worker.older-than-days")
  val sdesSubmissionRetryTimeout: Duration         = configuration.get[Duration]("sdes.submission.retry-after")
  val useXConversationIdHeader: Boolean            = configuration.get[Boolean]("features.use-x-conversation-id-header")
  val downloadDataSummaryReplaceIndexes: Boolean   = configuration.get[Boolean]("download-data-summary.replace-indexes")

  val pageSize: Int = configuration.get[Int]("pagination-config.recursive-page-size")
  val startingPage: Int =  configuration.get[Int]("pagination-config.recursive-starting-page")
  val localPageSize: Int =  configuration.get[Int]("pagination-config.local-page-size")
  val localStartingPage: Int =  configuration.get[Int]("pagination-config.local-starting-page")
}
