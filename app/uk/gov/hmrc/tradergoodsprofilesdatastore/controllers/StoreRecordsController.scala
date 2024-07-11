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

package uk.gov.hmrc.tradergoodsprofilesdatastore.controllers

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{CheckRecordsRepository, RecordsRepository}
import org.apache.pekko.Done

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.Request

class StoreRecordsController @Inject() (
  recordsRepository: RecordsRepository,
  checkRecordsRepository: CheckRecordsRepository,
  cc: ControllerComponents,
  routerConnector: RouterConnector,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def storeLatestRecords(
    eori: String
  ): Action[AnyContent] = identify.async { implicit request =>
    recordsRepository.getLatest(eori).flatMap {
      case Some(record) =>
        storeRecordsRecursively(eori, 1, Some(record.updatedDateTime.toString), 0)
          .map(_ => NoContent)
      case _            => Future.successful(NotFound)
    }
  }

  def storeAllRecords(
    eori: String
  ): Action[AnyContent] = identify.async { implicit request =>
    checkRecordsRepository
      .set(eori)
      .flatMap(_ => storeRecordsRecursively(eori, 1, None, 0).map(_ => NoContent))
  }

  private def storeRecordsRecursively(
    eori: String,
    page: Int,
    lastUpdatedDate: Option[String],
    numRecordsSaved: Int
  )(implicit request: Request[_]): Future[Done] =
    routerConnector.getRecords(eori, lastUpdatedDate, Some(page), Some(10000)).flatMap { recordsResponse =>
      recordsRepository.saveRecords(recordsResponse.goodsItemRecords).flatMap { _ =>
        val newNumRecordsSaved = recordsResponse.goodsItemRecords.size + numRecordsSaved
        if (newNumRecordsSaved >= recordsResponse.pagination.totalRecords) {
          recordsRepository.deleteInactive(eori).map(_ => Done)
        } else {
          storeRecordsRecursively(eori, page + 1, lastUpdatedDate, newNumRecordsSaved)
        }
      }
    }
}
