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
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GoodsItemRecord
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class StoreRecordsService @Inject() (
  routerConnector: RouterConnector,
  recordsRepository: RecordsRepository,
  recordsSummaryRepository: RecordsSummaryRepository,
  clock: Clock,
  config:DataStoreAppConfig
)(implicit
  ec: ExecutionContext
) extends Logging {

  def storeRecords(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[Boolean] = {

    val timeBeforeInitialCall = clock.instant()

    for {
      recordsResponse <- routerConnector.getRecords(eori, lastUpdatedDate, Some(config.startingPage), Some(config.pageSize))
      goodsRecords     = recordsResponse.goodsItemRecords
      totalRecords     = recordsResponse.pagination.totalRecords

      result <- processRecords(eori, goodsRecords, totalRecords, lastUpdatedDate, timeBeforeInitialCall)
    } yield result
  }
  private def handlePaginationAndSummaryUpdate(
    eori: String,
    totalRecords: Int,
    lastUpdatedDate: Option[String],
    initialTimestamp: Instant
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    if (totalRecords > config.pageSize) {
      storeRecordsRecursively(eori, config.startingPage + 1, lastUpdatedDate, config.pageSize)
        .flatMap(timeBeforeLastCall => recordsSummaryRepository.set(eori, None, timeBeforeLastCall).map(_ => true))
        .recover { case NonFatal(_) =>
          recordsSummaryRepository.update(eori, None, None)
          false
        }
    } else {
      recordsSummaryRepository.set(eori, None, initialTimestamp).map(_ => true)
    }

  private def processRecords(
    eori: String,
    goodsRecords: Seq[GoodsItemRecord],
    totalRecords: Int,
    lastUpdatedDate: Option[String],
    timeBeforeInitialCall: Instant
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    if (goodsRecords.isEmpty) {
      Future.successful(true)
    } else {
      for {
        _           <- recordsRepository.updateRecords(eori, goodsRecords)
        _           <- recordsSummaryRepository.set(
                         eori,
                         update = Some(Update(config.pageSize, totalRecords)),
                         lastUpdated = getLastUpdated(goodsRecords)
                       )
        finalResult <- handlePaginationAndSummaryUpdate(eori, totalRecords, lastUpdatedDate, timeBeforeInitialCall)
      } yield finalResult
    }

  private def storeRecordsRecursively(
    eori: String,
    page: Int,
    lastUpdatedDate: Option[String],
    recordsStored: Int
  )(implicit hc: HeaderCarrier): Future[Instant] = {
    val timeBeforeInitialCall = clock.instant()
    for {
      recordsResponse    <- routerConnector.getRecords(eori, lastUpdatedDate, Some(page), Some(config.pageSize))
      _                  <- recordsRepository.updateRecords(eori, recordsResponse.goodsItemRecords)
      newRecordsStored    = recordsStored + recordsResponse.goodsItemRecords.size
      _                  <- recordsSummaryRepository.set(
                              eori,
                              Some(Update(newRecordsStored, recordsResponse.pagination.totalRecords)),
                              getLastUpdated(recordsResponse.goodsItemRecords)
                            )
      timeBeforeLastCall <- if (recordsResponse.pagination.nextPage.isDefined) {
                              storeRecordsRecursively(eori, page + 1, lastUpdatedDate, newRecordsStored)
                            } else {
                              Future.successful(timeBeforeInitialCall)
                            }
    } yield timeBeforeLastCall
  }

  private def getLastUpdated(records: Seq[GoodsItemRecord]): Instant =
    records
      .maxBy(_.updatedDateTime)
      .updatedDateTime
}
