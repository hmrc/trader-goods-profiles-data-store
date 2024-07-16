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

import org.apache.pekko.Done
import play.api.mvc.Results.{NoContent, NotFound}

import javax.inject.Inject
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{recursivePageSize, recursiveStartingPage}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import scala.concurrent.{ExecutionContext, Future}

class StoreLatestActionImpl @Inject() (
  recordsRepository: RecordsRepository,
  routerConnector: RouterConnector
)(implicit val executionContext: ExecutionContext)
    extends StoreLatestAction {

  override protected def filter[A](
    identifierRequest: IdentifierRequest[A]
  ): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier             = HeaderCarrierConverter.fromRequest(identifierRequest)
    implicit val request: IdentifierRequest[A] = identifierRequest

    recordsRepository.getLatest(identifierRequest.eori).flatMap {
      case Some(record) =>
        storeRecordsRecursively(identifierRequest.eori, recursiveStartingPage, Some(record.updatedDateTime.toString))
          .map(_ => None)
      case _            => Future.successful(Some(NotFound))
    }
  }

  private def storeRecordsRecursively(
    eori: String,
    page: Int,
    lastUpdatedDate: Option[String]
  )(implicit request: Request[_], hc: HeaderCarrier): Future[Done] =
    routerConnector.getRecords(eori, lastUpdatedDate, Some(page), Some(recursivePageSize)).flatMap { recordsResponse =>
      recordsRepository.saveRecords(eori, recordsResponse.goodsItemRecords).flatMap { _ =>
        if (recordsResponse.goodsItemRecords.length == recursivePageSize) {
          storeRecordsRecursively(eori, page + 1, lastUpdatedDate)
        } else {
          recordsRepository.deleteInactive(eori).map(_ => Done)
        }
      }
    }
}

trait StoreLatestAction extends ActionFilter[IdentifierRequest]
