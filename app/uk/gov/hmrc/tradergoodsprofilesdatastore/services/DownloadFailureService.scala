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

package uk.gov.hmrc.tradergoodsprofilesdatastore.services

import org.apache.pekko.Done
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.DownloadRecordFailureEmailParameters
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import play.api.Logging
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.Clock
import java.time.temporal.ChronoUnit

@Singleton
class DownloadFailureService @Inject() (
  customsDataStoreConnector: CustomsDataStoreConnector,
  emailConnector: EmailConnector,
  downloadDataSummaryRepository: DownloadDataSummaryRepository,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends Logging {

  def processStaleDownloads(): Future[Done] = {
    val slaThreshold               = clock.instant().minus(24, ChronoUnit.HOURS)
    implicit val hc: HeaderCarrier = HeaderCarrier()

    downloadDataSummaryRepository
      .findStaleSummaries(slaThreshold)
      .flatMap { staleSummaries =>
        if (staleSummaries.isEmpty) {
          Future.successful(Done)
        } else {
          val eoris = staleSummaries.map(_.eori)
          Future
            .sequence(eoris.map { eori =>
              downloadDataSummaryRepository.markAsFailed(eori).flatMap { count =>
                if (count > 0) {
                  sendFailureEmailNotification(eori)
                } else {
                  Future.successful(Done)
                }
              }
            })
            .map(_ => Done)
        }
      }
      .recover { case e =>
        logger.error(s"[DownloadFailureService] - Error processing stale downloads: ${e.getMessage}", e)
        Done
      }
  }

  private def sendFailureEmailNotification(eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    customsDataStoreConnector
      .getEmail(eori)
      .flatMap {
        case Some(email) =>
          emailConnector.sendDownloadRecordFailureEmail(
            email.address,
            DownloadRecordFailureEmailParameters()
          )
        case None        =>
          logger.warn(s"[DownloadFailureService] - Unable to find email for EORI: $eori")
          Future.failed(new RuntimeException(s"Unable to find email for EORI: $eori"))
      }
      .recover { case e =>
        logger.warn(s"[DownloadFailureService] - Failed to send failure email for EORI $eori: ${e.getMessage}")
        Done
      }
}
