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
import uk.gov.hmrc.http.{HeaderCarrier, PreconditionFailedException}
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{recursivePageSize, recursiveStartingPage}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import scala.concurrent.{ExecutionContext, Future}

class StoreRecordsService @Inject() (routerConnector: RouterConnector, recordsRepository: RecordsRepository)(implicit
  ec: ExecutionContext
) extends Logging {

  def storeRecords(
    eori: String,
    lastUpdatedDate: Option[String]
  )(implicit request: Request[_], hc: HeaderCarrier): Future[Done] =
    storeRecordsRecursively(eori, recursiveStartingPage, lastUpdatedDate).flatMap { totalRouterRecords =>
      recordsRepository.getCountWithInactive(eori).flatMap { totalDataStoreRecords =>
        if (totalRouterRecords == totalDataStoreRecords) {
          Future.successful(Done)
        } else {
          Future.failed(
            new RuntimeException(
              s"Data Store and B&T Database are out of sync: There are $totalDataStoreRecords data store records and $totalRouterRecords B&T records."
            )
          )
        }
      }
    }

  def deleteAndStoreRecords(
    eori: String
  )(implicit request: Request[_], hc: HeaderCarrier): Future[Done] =
    recordsRepository.deleteMany(eori).flatMap { _ =>
      storeRecordsRecursively(eori, recursiveStartingPage, None).map(_ => Done)
    }

  private def storeRecordsRecursively(
    eori: String,
    page: Int,
    lastUpdatedDate: Option[String]
  )(implicit request: Request[_], hc: HeaderCarrier): Future[Long] =
    routerConnector.getRecords(eori, lastUpdatedDate, Some(page), Some(recursivePageSize)).flatMap { recordsResponse =>
      recordsRepository.saveRecords(eori, recordsResponse.goodsItemRecords).flatMap { _ =>
        if (recordsResponse.pagination.nextPage.isDefined) {
          storeRecordsRecursively(eori, page + 1, lastUpdatedDate)
        } else {
          Future.successful(recordsResponse.pagination.totalRecords)
        }
      }
    }
}
