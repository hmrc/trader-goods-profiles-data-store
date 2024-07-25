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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{recursivePageSize, recursiveStartingPage}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}

import scala.concurrent.{ExecutionContext, Future}

class StoreRecordsService @Inject() (
  routerConnector: RouterConnector,
  recordsRepository: RecordsRepository,
  recordsSummaryRepository: RecordsSummaryRepository
)(implicit
  ec: ExecutionContext
) extends Logging {

  def cleanseCache(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    lastUpdatedDate match {
      case Some(_) => Future.successful(false)
      case None    => recordsRepository.deleteManyByEori(eori).map(_ => true)
    }

  def storeRecords(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    cleanseCache(eori, lastUpdatedDate).flatMap { _ =>
      storeFirstBatchOfRecords(eori, lastUpdatedDate).flatMap { recordsToStore =>
        if (recordsToStore > recursivePageSize) {
          recordsSummaryRepository.set(eori, Some(Update(recursivePageSize, recordsToStore - recursivePageSize))).map {
            _ =>
              storeRecordsRecursively(eori, recursiveStartingPage + 1, lastUpdatedDate, 0, recordsToStore).onComplete {
                _ =>
                  recordsSummaryRepository.set(eori, None).flatMap { _ =>
                    checkInSync(eori)
                  }
              }
              false
          }
        } else {
          checkInSync(eori).map(_ => true)
        }
      }
    }

  private def checkInSync(eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    routerConnector.getRecords(eori, None, Some(recursiveStartingPage), Some(1)).flatMap { recordsResponse =>
      val totalRouterRecords = recordsResponse.pagination.totalRecords
      recordsRepository.getCountWithInactive(eori).map { totalDataStoreRecords =>
        if (totalRouterRecords != totalDataStoreRecords) {
          logger.error(
            s"Data Store and B&T Database are out of sync for eori $eori: There are $totalDataStoreRecords data store records and $totalRouterRecords B&T records."
          )
        }
        Done
      }
    }

  //TODO delete function
  def deleteAndStoreRecords(
    eori: String
  )(implicit hc: HeaderCarrier): Future[Done] =
    recordsRepository.deleteManyByEori(eori).flatMap { _ =>
      storeRecordsRecursively(eori, recursiveStartingPage, None, 0, 0).map(_ => Done)
    }

  private def storeFirstBatchOfRecords(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[Int] =
    routerConnector.getRecords(eori, lastUpdatedDate, Some(recursiveStartingPage), Some(recursivePageSize)).flatMap {
      recordsResponse =>
        recordsRepository
          .saveRecords(eori, recordsResponse.goodsItemRecords)
          .map(_ => recordsResponse.pagination.totalRecords)
    }

  private def storeRecordsRecursively(
    eori: String,
    page: Int,
    lastUpdatedDate: Option[String],
    recordsToStore: Int,
    recordsStored: Int
  )(implicit hc: HeaderCarrier): Future[Done] =
    routerConnector.getRecords(eori, lastUpdatedDate, Some(page), Some(recursivePageSize)).flatMap { recordsResponse =>
      recordsRepository.saveRecords(eori, recordsResponse.goodsItemRecords).flatMap { _ =>
        val newRecordsToStore = recordsToStore - recordsResponse.goodsItemRecords.size
        val newRecordsStored  = recordsStored + recordsResponse.goodsItemRecords.size
        recordsSummaryRepository.set(eori, Some(Update(newRecordsToStore, newRecordsStored))).flatMap { _ =>
          if (recordsResponse.pagination.nextPage.isDefined) {
            storeRecordsRecursively(eori, page + 1, lastUpdatedDate, newRecordsToStore, newRecordsStored)
          } else {
            recordsRepository.deleteManyByEoriAndInactive(eori).map(_ => Done)
          }
        }
      }
    }
}
