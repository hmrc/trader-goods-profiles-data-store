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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{IdentifierAction, StoreLatestAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.buildPagination
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GetRecordsController @Inject() (
  routerConnector: RouterConnector,
  recordsRepository: RecordsRepository,
  cc: ControllerComponents,
  identify: IdentifierAction,
  storeLatest: StoreLatestAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getLocalRecords(
    eori: String,
    pageOpt: Option[Int],
    sizeOpt: Option[Int]
  ): Action[AnyContent] = (identify andThen storeLatest).async {
    recordsRepository.getCount(eori).flatMap { totalRecords =>
      recordsRepository.getMany(eori, pageOpt, sizeOpt).map { records =>
        val getRecordsResponse =
          GetRecordsResponse(goodsItemRecords = records, buildPagination(sizeOpt, pageOpt, totalRecords))
        Ok(Json.toJson(getRecordsResponse))
      }
    }
  }

  def getRecord(eori: String, recordId: String): Action[AnyContent] = identify.async { implicit request =>
    routerConnector
      .getRecord(eori, recordId)
      .map {
        case Some(record) if record.active => Ok(Json.toJson(record))
        case _                             => NotFound
      }
  }

}
