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

import com.typesafe.config.ConfigFactory
import play.api.libs.json.{Json, OFormat}

case class Pagination(
  totalRecords: Int,
  currentPage: Int,
  totalPages: Int,
  nextPage: Option[Int],
  prevPage: Option[Int]
)

object Pagination {

  implicit val format: OFormat[Pagination] = Json.format[Pagination]

  val recursivePageSize: Int     = ConfigFactory.load().getInt("pagination-config.recursive-page-size")
  val recursiveStartingPage: Int = ConfigFactory.load().getInt("pagination-config.recursive-starting-page")
  val localPageSize: Int         = ConfigFactory.load().getInt("pagination-config.local-page-size")
  val localStartingPage: Int     = ConfigFactory.load().getInt("pagination-config.local-starting-page")

  def buildPagination(sizeOpt: Option[Int], pageOpt: Option[Int], totalRecords: Long): Pagination = {
    val size                 = sizeOpt.getOrElse(localPageSize)
    val page                 = pageOpt.getOrElse(localStartingPage)
    val mod                  = totalRecords % size
    val totalRecordsMinusMod = totalRecords - mod
    val totalPages           = (if (mod == 0 && totalRecords != 0) {
                        totalRecordsMinusMod / size
                      } else {
                        (totalRecordsMinusMod / size) + 1
                      }).toInt
    val nextPage             = if (page >= totalPages || page < 1) None else Some(page + 1)
    val prevPage             = if (page <= 1 || page > totalPages) None else Some(page - 1)
    Pagination(totalRecords.toInt, page, totalPages, nextPage, prevPage)
  }
}
