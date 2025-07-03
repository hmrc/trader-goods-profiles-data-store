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

import com.google.inject.Inject
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GoodsItemRecord
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.PaginationHelper

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class StoreRecordsService @Inject() (
                                      routerConnector: RouterConnector,
                                      recordsRepository: RecordsRepository,
                                      recordsSummaryRepository: RecordsSummaryRepository,
                                      clock: Clock,
                                      paginationHelper: PaginationHelper
                                    )(implicit
                                      ec: ExecutionContext
                                    ) extends Logging {

  private val pageSize = paginationHelper.localPageSize
  private val startingPage = paginationHelper.localStartingPage

  def storeRecords(
                    eori: String,
                    lastUpdatedDate: Option[String]
                  )(implicit hc: HeaderCarrier): Future[Boolean] = {

    val timeBeforeInitialCall = clock.instant()

    val firstPageResult = for {
      recordsResponse <- routerConnector.getRecords(eori, lastUpdatedDate, Some(startingPage), Some(pageSize))
      goodsRecords = recordsResponse.goodsItemRecords
      totalRecords = recordsResponse.pagination.totalRecords

      processResult <- processRecords(eori, goodsRecords, totalRecords, lastUpdatedDate, timeBeforeInitialCall)
      (_, recordsStored) = processResult

      // Immediately update the summary after first page processed
      _ <- recordsSummaryRepository.set(eori, Some(Update(recordsStored, totalRecords)), clock.instant())

      // If there is a next page, continue recursively from it, else finish with success
      finalResult <- recordsResponse.pagination.nextPage match {
        case Some(nextPage) =>
          handlePaginationAndSummaryUpdate(eori, nextPage, recordsStored)
        case None =>
          Future.successful(true)
      }
    } yield finalResult

    firstPageResult.recoverWith {
      case NonFatal(e) =>
        logger.warn(s"Failed to store records for $eori: ${e.getMessage}", e)
        recordsSummaryRepository.update(eori, None, None).map(_ => false)
    }
  }

  private def handlePaginationAndSummaryUpdate(
                                                eori: String,
                                                currentPage: Int,
                                                accumulatedCount: Int
                                              )(implicit hc: HeaderCarrier): Future[Boolean] = {

    def recursiveCall(page: Int, accumulatedCount: Int, lastUpdated: Option[Instant]): Future[Boolean] = {
      routerConnector.getRecords(eori, None, Some(page), Some(pageSize)).flatMap { response =>
        val goodsRecords = response.goodsItemRecords
        val totalRecords = response.pagination.totalRecords

        val newLastUpdated = goodsRecords.foldLeft(lastUpdated) { (max, r) =>
          Some(max.fold(r.updatedDateTime)(d => if (r.updatedDateTime.isAfter(d)) r.updatedDateTime else d))
        }

        processRecords(eori, goodsRecords, totalRecords, None, Instant.now()).flatMap {
          case (true, count) =>
            val updatedCount = accumulatedCount + count
            val updatedSummary = Some(Update(updatedCount, totalRecords))

            recordsSummaryRepository.set(
              eori,
              updatedSummary,
              newLastUpdated.getOrElse(clock.instant())
            ).flatMap { _ =>
              response.pagination.nextPage match {
                case Some(nextPage) =>
                  recursiveCall(nextPage, updatedCount, newLastUpdated)
                case None =>
                  // Final cleanup if no more pages
                  recordsSummaryRepository.set(eori, None, clock.instant()).map(_ => true)
              }
            }

          case (false, _) =>
            // Processing failed, clear summary and return false
            recordsSummaryRepository.update(eori, None, None).map(_ => false)
        }
      }.recoverWith {
        case NonFatal(e) =>
          logger.warn(s"Failed to fetch page $page for $eori: ${e.getMessage}")
          recordsSummaryRepository.update(eori, None, None).map(_ => false)
      }
    }

    recursiveCall(currentPage, accumulatedCount, None)
  }

  private def processRecords(
                              eori: String,
                              goodsRecords: Seq[GoodsItemRecord],
                              totalRecords: Int,
                              lastUpdatedDate: Option[String],
                              timeBeforeInitialCall: Instant
                            )(implicit hc: HeaderCarrier): Future[(Boolean, Int)] = {

    if (goodsRecords.isEmpty) {
      // No update for empty page; return success with 0 count
      Future.successful((true, 0))
    } else {
      // Update records repository for non-empty list
      recordsRepository.updateRecords(eori, goodsRecords).map(_ => (true, goodsRecords.size))
    }
  }
}
