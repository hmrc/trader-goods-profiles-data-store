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
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{CheckRecordsRepository, RecordsRepository}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GetRecordsController @Inject() (
  checkRecordsRepository: CheckRecordsRepository,
  routerConnector: RouterConnector,
  recordsRepository: RecordsRepository,
  cc: ControllerComponents,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getLocalRecords(
    eori: String,
    pageOpt: Option[Int],
    sizeOpt: Option[Int]
  ): Action[AnyContent] = identify.async { implicit request =>
    recordsRepository.getCount(eori).flatMap { totalRecords =>
      recordsRepository.getMany(eori, pageOpt, sizeOpt).map { records =>
        val getRecordsResponse =
          GetRecordsResponse(goodsItemRecords = records, buildPagination(sizeOpt, pageOpt, totalRecords))
        Ok(Json.toJson(getRecordsResponse))
      }
    }
  }

  def getRecordsCount(
    eori: String
  ): Action[AnyContent] = identify.async { implicit request =>
    routerConnector.getRecords(eori).map { httpResponse =>
      val recordsResponse = httpResponse.json.as[GetRecordsResponse]
      Ok(Json.toJson(recordsResponse.pagination.totalRecords))
    }
  }

  def buildPagination(sizeOpt: Option[Int], pageOpt: Option[Int], totalRecords: Double): Pagination = {
    val size       = sizeOpt.getOrElse(10)
    val page       = pageOpt.getOrElse(1)
    val mod        = totalRecords % size
    val totalPages = ((totalRecords - mod) / size).toInt
    val nextPage   = if (page == totalPages) None else Some(page + 1)
    val prevPage   = if (page == 1) None else Some(page - 1)
    Pagination(totalRecords.toInt, page, totalPages, nextPage, prevPage)
  }

  def checkRecords(eori: String): Action[AnyContent] = identify.async { implicit request =>
    checkRecordsRepository
      .get(eori)
      .map {
        case Some(_) => Ok
        case None    => NotFound
      }
  }
}
