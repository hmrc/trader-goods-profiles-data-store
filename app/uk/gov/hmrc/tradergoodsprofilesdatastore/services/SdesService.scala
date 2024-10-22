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

import org.apache.pekko.Done
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.SdesSubmissionWorkItem
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.DownloadRecordEmailParameters
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.SdesSubmissionWorkItemRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.DateTimeFormats.dateTimeFormat

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SdesService @Inject() (
  customsDataStoreConnector: CustomsDataStoreConnector,
  emailConnector: EmailConnector,
  config: DataStoreAppConfig,
  clock: Clock,
  workItemRepository: SdesSubmissionWorkItemRepository
)(implicit ec: ExecutionContext) {

  def enqueueSubmission(submissionId: String, eori: String, retentionDays: String, summaryId: String): Future[Done] = {

    val workItem = SdesSubmissionWorkItem(
      submissionId = submissionId,
      eori = eori,
      retentionDays = retentionDays,
      summaryId = summaryId
    )

    workItemRepository.pushNew(workItem, clock.instant()).map(_ => Done)
  }

  def processNextSubmission(): Future[Boolean] = {
    val now = clock.instant()
    workItemRepository.pullOutstanding(now.minus(config.sdesSubmissionRetryTimeout), now).flatMap {
      _.map { workItem =>
        for {
          _ <- sendEmailNotification(workItem.item.eori, workItem.item.retentionDays).recoverWith { case e =>
                 workItemRepository.markAs(workItem.id, ProcessingStatus.Failed).flatMap { _ =>
                   Future.failed(e)
                 }
               }
          _ <- workItemRepository.complete(workItem.id, ProcessingStatus.Succeeded)
        } yield true
      }.getOrElse(Future.successful(false))
    }
  }

  private def sendEmailNotification(eori: String, retentionDays: String): Future[Done] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    //TODO determine when to send email in english or welsh (default is english) TGP-2654
    val isWelsh                    = false
    //TODO add flag into qa config as false until the email stuff is working
    if (config.sendNotificationEmail) {
      customsDataStoreConnector.getEmail(eori).flatMap {
        case Some(email) =>
          emailConnector
            .sendDownloadRecordEmail(
              email.address,
              DownloadRecordEmailParameters(
                convertToDateString(clock.instant.plus(retentionDays.toInt, ChronoUnit.DAYS), isWelsh)
              )
            )
        case None        => Future.failed(new RuntimeException(s"Unable to find the email for EORI: $eori"))
      }
    } else {
      Future.successful(Done)
    }
  }

  private def convertToDateString(instant: Instant, isWelsh: Boolean): String =
    instant
      .atZone(ZoneOffset.UTC)
      .toLocalDate
      .format(dateTimeFormat(if (isWelsh) { "cy" }
      else { "en" }))

  def processAllSubmissions(): Future[Done] =
    processNextSubmission().flatMap {
      case true  =>
        processAllSubmissions()
      case false =>
        Future.successful(Done)
    }
}
