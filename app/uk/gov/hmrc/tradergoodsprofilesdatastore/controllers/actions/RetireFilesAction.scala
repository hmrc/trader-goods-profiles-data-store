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

package uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions

import play.api.Logging
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Ordering.Implicits.infixOrderingOps

class RetireFilesActionImpl @Inject() (downloadDataSummaryRepository: DownloadDataSummaryRepository)(implicit
  val executionContext: ExecutionContext
) extends RetireFilesAction
    with Logging {

  override protected def filter[A](
    identifierRequest: IdentifierRequest[A]
  ): Future[Option[Result]] =
    downloadDataSummaryRepository.get(identifierRequest.eori).flatMap { downloadDataSummaries =>
      val summaryIdsToRemove = downloadDataSummaries
        .filter { downloadDataSummary =>
          downloadDataSummary.fileInfo match {
            case Some(info) if info.fileCreated < Instant.now.minus(info.retentionDays.toInt, ChronoUnit.DAYS) =>
              true
            case _                                                                                             => false
          }
        }
        .map(summary => summary.summaryId)

      downloadDataSummaryRepository.deleteMany(identifierRequest.eori, summaryIdsToRemove).map { _ =>
        None
      }
    }
}

trait RetireFilesAction extends ActionFilter[IdentifierRequest]
