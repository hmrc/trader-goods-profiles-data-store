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
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FilterRecordsController @Inject() (
  storeRecordsController: StoreRecordsController,
  recordsRepository: RecordsRepository,
  cc: ControllerComponents,
  identify: IdentifierAction,
  config: DataStoreAppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def filterLocalRecords(
    eori: String,
    searchTerm: Option[String],
    field: Option[String],
    pageOpt: Option[Int],
    sizeOpt: Option[Int]
  ): Action[AnyContent] =
    identify.async { implicit request =>
      for {
        _               <- storeRecordsController.storeLatestRecords(eori).apply(request)
        filteredRecords <- recordsRepository.filterRecords(eori, searchTerm, field)
      } yield {
        val size               = sizeOpt.getOrElse(config.pageSize)
        val page               = pageOpt.getOrElse(config.startingPage)
        val skip               = (page - 1) * size
        val paginatedRecords   = filteredRecords.drop(skip).take(size)
        val pagination         = buildPagination(size, page, filteredRecords.size.toLong)
        val getRecordsResponse = GetRecordsResponse(goodsItemRecords = paginatedRecords, pagination = pagination)
        Ok(Json.toJson(getRecordsResponse))
      }
    }

  private def buildPagination(size: Int, page: Int, totalRecords: Long): Pagination = {
    val mod                  = totalRecords % size
    val totalRecordsMinusMod = totalRecords - mod
    val totalPages           = ((totalRecordsMinusMod / size) + 1).toInt
    val nextPage             = if (page >= totalPages || page < 1) None else Some(page + 1)
    val prevPage             = if (page <= 1 || page > totalPages) None else Some(page - 1)
    Pagination(totalRecords.toInt, page, totalPages, nextPage, prevPage)
  }
}