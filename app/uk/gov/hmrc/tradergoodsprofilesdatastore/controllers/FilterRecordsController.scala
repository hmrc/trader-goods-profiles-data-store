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
import scala.concurrent.{ExecutionContext, Future}

class FilterRecordsController @Inject() (
  recordsRepository: RecordsRepository,
  cc: ControllerComponents,
  identify: IdentifierAction,
  storeLatest: StoreLatestAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def filterLocalRecords(
    eori: String,
    searchTerm: Option[String],
    exactMatch: Option[Boolean],
    field: Option[String],
    pageOpt: Option[Int],
    sizeOpt: Option[Int]
  ): Action[AnyContent] =
    (identify andThen storeLatest).async {
      val validFields  = Set("traderRef", "goodsDescription", "comcode")
      val isExactMatch = exactMatch.getOrElse(true)

      field match {
        case Some(value) if !validFields.contains(value) =>
          Future.successful(BadRequest("Invalid field parameter"))
        case _                                           =>
          for {
            filteredRecords <- recordsRepository.filterRecords(eori, searchTerm, field, isExactMatch)
          } yield {
            val size             = sizeOpt.getOrElse(localPageSize)
            val page             = pageOpt.getOrElse(localStartingPage)
            val skip             = (page - 1) * size
            val paginatedRecords = filteredRecords.slice(skip, skip + size)

            val pagination         = buildPagination(Some(size), Some(page), filteredRecords.size.toLong)
            val getRecordsResponse = GetRecordsResponse(goodsItemRecords = paginatedRecords, pagination = pagination)
            Ok(Json.toJson(getRecordsResponse))
          }
      }
    }

  def filterIteration( // TODO: Rename this to filterLocalRecords when frontend changes are implemented and delete the old logic relating to old filtering (TGP-3003)
    searchTerm: Option[String],
    countryOfOrigin: Option[String],
    IMMIReady: Option[Boolean],
    notReadyForIMMI: Option[Boolean],
    actionNeeded: Option[Boolean],
    pageOpt: Option[Int],
    sizeOpt: Option[Int]
  ): Action[AnyContent] =
    (identify andThen storeLatest).async { request =>
      val eori = request.eori

      for {
        filteredRecords <-
          recordsRepository
            .filterRecordsIteration(eori, searchTerm, countryOfOrigin, IMMIReady, notReadyForIMMI, actionNeeded)
      } yield {
        val size             = sizeOpt.getOrElse(localPageSize)
        val page             = pageOpt.getOrElse(localStartingPage)
        val skip             = (page - 1) * size
        val paginatedRecords = filteredRecords.slice(skip, skip + size)

        val pagination         = buildPagination(Some(size), Some(page), filteredRecords.size.toLong)
        val getRecordsResponse = GetRecordsResponse(goodsItemRecords = paginatedRecords, pagination = pagination)
        Ok(Json.toJson(getRecordsResponse))
      }
    }

  def isTraderReferenceUnique(traderReference: String): Action[AnyContent] =
    (identify andThen storeLatest).async { implicit request =>
      recordsRepository.isTraderReferenceUnique(request.eori, traderReference).map {
        case true  => Ok(Json.obj("isUnique" -> true))
        case false => Ok(Json.obj("isUnique" -> false))
      }
    }
}
