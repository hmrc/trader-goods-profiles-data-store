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
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{recursivePageSize, recursiveStartingPage}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{CheckRecordsRepository, RecordsRepository}

import scala.concurrent.{ExecutionContext, Future}

class StoreRecordsService @Inject() (
  routerConnector: RouterConnector,
  recordsRepository: RecordsRepository,
  checkRecordsRepository: CheckRecordsRepository
)(implicit
  ec: ExecutionContext
) extends Logging {

  def storeRecords(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    storeFirstBatchOfRecords(eori, lastUpdatedDate).flatMap { isMoreData =>
      if (isMoreData) {
        println("isMoreData")
        checkRecordsRepository.set(eori, recordsUpdating = true).map { _ =>
          println("checkRecordsRepository.set")
          println(true)

          storeRecordsRecursively(eori, recursiveStartingPage + 1, lastUpdatedDate).onComplete { _ =>
            checkRecordsRepository.set(eori, recordsUpdating = false).flatMap { _ =>
              println("checkRecordsRepository.set")
              println(false)
              checkInSync(eori)
            }
          }
          false
        }
      } else {
        checkInSync(eori).map(_ => true)
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

  def deleteAndStoreRecords(
    eori: String
  )(implicit hc: HeaderCarrier): Future[Done] =
    recordsRepository.deleteMany(eori).flatMap { _ =>
      storeRecordsRecursively(eori, recursiveStartingPage, None).map(_ => Done)
    }

  private def storeFirstBatchOfRecords(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    routerConnector.getRecords(eori, lastUpdatedDate, Some(recursiveStartingPage), Some(recursivePageSize)).flatMap {
      recordsResponse =>
        recordsRepository
          .saveRecords(eori, recordsResponse.goodsItemRecords)
          .map(_ => recordsResponse.pagination.nextPage.isDefined)
    }

  private def storeRecordsRecursively(
    eori: String,
    page: Int,
    lastUpdatedDate: Option[String]
  )(implicit hc: HeaderCarrier): Future[Done] =
    routerConnector.getRecords(eori, lastUpdatedDate, Some(page), Some(recursivePageSize)).flatMap { recordsResponse =>
      recordsRepository.saveRecords(eori, recordsResponse.goodsItemRecords).flatMap { _ =>
        println("storeRecordsRecursively")
        println("eori")
        println(eori)
        println("page")
        println(page)
        println(recordsResponse.pagination)

        if (recordsResponse.pagination.nextPage.isDefined) {
          storeRecordsRecursively(eori, page + 1, lastUpdatedDate)
        } else {
          Future.successful(Done)
        }
      }
    }
}
