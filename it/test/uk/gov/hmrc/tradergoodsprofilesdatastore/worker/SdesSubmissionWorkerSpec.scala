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

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.mockito.Mockito
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.FileReadyUnseen
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.DownloadRecordEmailParameters
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Email
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.SdesSubmissionWorkItemRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.DateTimeFormats.convertToDateString

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{Future, Promise}

class SdesSubmissionWorkerSpec
    extends AnyFreeSpec
    with Matchers
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite
    with CleanMongoCollectionSupport
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with Eventually {

  private val mockCustomsDataStoreConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]
  private val mockEmailConnector: EmailConnector                       = mock[EmailConnector]

  private val id  = java.util.UUID.randomUUID().toString
  private val now = Instant.now

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector),
      bind[EmailConnector].toInstance(mockEmailConnector)
    )
    .configure(
      "workers.sdes-submission.initial-delay" -> "1s",
      "workers.sdes-submission.interval"      -> "1s"
    )
    .build()

  private val submissionWorkerItemRepository: SdesSubmissionWorkItemRepository =
    app.injector.instanceOf[SdesSubmissionWorkItemRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockCustomsDataStoreConnector)
    Mockito.reset(mockEmailConnector)
    submissionWorkerItemRepository.initialised.futureValue
  }

  "must process waiting submissions" in {
    val eori          = "GB1234567890"
    val retentionDays = "30"
    val fileName      = "fileName"
    val fileSize      = 600
    val timestamp     = Instant.now
    val address       = "email@test.co.uk"
    val email         = Email(address, timestamp)

    val summary = DownloadDataSummary(
      id,
      eori,
      FileReadyUnseen,
      now,
      now.plus(retentionDays.toInt, ChronoUnit.DAYS),
      Some(FileInfo(fileName, fileSize, retentionDays))
    )

    val downloadRecordEmailParameters = DownloadRecordEmailParameters(
      convertToDateString(now.plus(retentionDays.toInt, ChronoUnit.DAYS), isWelsh = false)
    )
    when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(Some(email))
    when(mockEmailConnector.sendDownloadRecordEmail(any(), any())(any())) thenReturn Future.successful(Done)
    submissionWorkerItemRepository.pushNew(summary).futureValue

    val done = Promise[Done]
    eventually {
      verify(mockCustomsDataStoreConnector).getEmail(eqTo(eori))(any())
      verify(mockEmailConnector).sendDownloadRecordEmail(eqTo(email.address), eqTo(downloadRecordEmailParameters))(
        any()
      )
      done.success(Done)
    }
  }
}
