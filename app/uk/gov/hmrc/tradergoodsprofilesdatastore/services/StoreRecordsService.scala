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
import org.apache.pekko.Done
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, GoodsItemRecord}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{pageSize, startingPage}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

class StoreRecordsService @Inject() (
  routerConnector: RouterConnector,
  recordsRepository: RecordsRepository,
  recordsSummaryRepository: RecordsSummaryRepository,
  clock: Clock
)(implicit
  ec: ExecutionContext
) extends Logging {

  def storeRecords(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    storeFirstBatchOfRecords(eori, lastUpdatedDate).flatMap { recordsResponse =>
      val totalRecords = recordsResponse.pagination.totalRecords
      if (totalRecords > pageSize) {

        recordsSummaryRepository.set(
          eori,
          update = Some(Update(pageSize, totalRecords - pageSize)),
          lastUpdated = getLastUpdated(recordsResponse.goodsItemRecords)
        ).map { _ =>
          storeRecordsRecursively(
            eori,
            startingPage + 1,
            lastUpdatedDate,
            totalRecords - pageSize,
            pageSize
          ).onComplete { _ =>
            recordsSummaryRepository.update(eori, None, None)
          }

          false
        }
      } else if (totalRecords > 0) {
        recordsSummaryRepository
          .set(eori, None, getLastUpdated(recordsResponse.goodsItemRecords))
          .map(_ => true)
      } else {
        Future.successful(true)
      }
    }

  private def storeFirstBatchOfRecords(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[GetRecordsResponse] =
    routerConnector.getRecords(eori, lastUpdatedDate, Some(startingPage), Some(pageSize)).flatMap { recordsResponse =>
      if (recordsResponse.goodsItemRecords.nonEmpty) {
        recordsRepository.updateRecords(eori, recordsResponse.goodsItemRecords)
          .map(_ => recordsResponse)
      } else {
        Future.successful(recordsResponse)
      }
    }

  private def storeRecordsRecursively(
    eori: String,
    page: Int,
    lastUpdatedDate: Option[String],
    recordsToStore: Int,
    recordsStored: Int
  )(implicit hc: HeaderCarrier): Future[Seq[GoodsItemRecord]] = {

    for {
      recordsResponse   <- routerConnector.getRecords(eori, lastUpdatedDate, Some(page), Some(pageSize))
      _                 <- recordsRepository.updateRecords(eori, recordsResponse.goodsItemRecords)
      newRecordsToStore =  recordsToStore - recordsResponse.goodsItemRecords.size
      newRecordsStored  =  recordsStored + recordsResponse.goodsItemRecords.size
      _                 <- recordsSummaryRepository.set(eori, Some(Update(newRecordsStored, newRecordsToStore)), getLastUpdated(recordsResponse.goodsItemRecords))
      _                 <- if (recordsResponse.pagination.nextPage.isDefined) {
                             storeRecordsRecursively(eori, page + 1, lastUpdatedDate, newRecordsStored, newRecordsToStore)
                           } else {
                             Future.successful(Done)
                           }
    } yield recordsResponse.goodsItemRecords
  }

  private def getLastUpdated(records: Seq[GoodsItemRecord]): Instant =
    records
      .maxByOption(_.updatedDateTime)
      .map(_.updatedDateTime)
      .getOrElse(clock.instant())
}
