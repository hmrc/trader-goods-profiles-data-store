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
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{IdentifierAction, StoreLatestAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{buildPagination, localPageSize, localStartingPage}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SearchRecordsAndApplyFiltersController @Inject()(
  recordsRepository: RecordsRepository,
  cc: ControllerComponents,
  identify: IdentifierAction,
  storeLatest: StoreLatestAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def searchAndFilter(
    eori: String,
    searchTerm: Option[String],
    adviceStatus: Option[String],
    countryOfOrigin: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Action[AnyContent] = (identify andThen storeLatest).async { implicit request =>
    for {
      filteredRecords <- recordsRepository.searchRecordsAndApplyFilters(eori, searchTerm, adviceStatus, countryOfOrigin)
    } yield {
      val sizeOrDefault             = size.getOrElse(localPageSize)
      val pageOrDefault            = page.getOrElse(localStartingPage)
      val skip             = (pageOrDefault - 1) * sizeOrDefault
      val paginatedRecords = filteredRecords.slice(skip, skip + sizeOrDefault)

      val pagination         = buildPagination(Some(sizeOrDefault), Some(pageOrDefault), filteredRecords.size.toLong)
      val getRecordsResponse = GetRecordsResponse(goodsItemRecords = paginatedRecords, pagination = pagination)
      Ok(Json.toJson(getRecordsResponse))
    }
  }
}
