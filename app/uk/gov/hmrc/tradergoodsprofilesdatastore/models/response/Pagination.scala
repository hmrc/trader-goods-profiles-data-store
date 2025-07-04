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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models.response

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig

case class Pagination(
  totalRecords: Int,
  currentPage: Int,
  totalPages: Int,
  nextPage: Option[Int],
  prevPage: Option[Int]
)

object Pagination {

  implicit val format: OFormat[Pagination] = Json.format[Pagination]

  def buildPagination(
    sizeOpt: Option[Int],
    pageOpt: Option[Int],
    totalRecords: Long,
    config: DataStoreAppConfig
  ): Pagination = {

    val size          = sizeOpt.getOrElse(config.localPageSize)
    val requestedPage = pageOpt.getOrElse(config.localStartingPage)

    val mod                  = totalRecords % size
    val totalRecordsMinusMod = totalRecords - mod
    val totalPages           = {
      if (mod == 0 && totalRecords != 0) {
        totalRecordsMinusMod / size
      } else {
        (totalRecordsMinusMod / size) + 1
      }
    }.toInt

    // Clamp page to valid range: min 1, max totalPages (if totalPages > 0)
    val page =
      if (totalPages == 0) 1
      else if (requestedPage < 1) 1
      else if (requestedPage > totalPages) totalPages
      else requestedPage

    val nextPage = if (page >= totalPages) None else Some(page + 1)
    val prevPage = if (page <= 1) None else Some(page - 1)

    Pagination(totalRecords.toInt, page, totalPages, nextPage, prevPage)
  }
}
