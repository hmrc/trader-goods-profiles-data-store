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

package uk.gov.hmrc.tradergoodsprofilesdatastore.utils

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models._
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Assessment, Condition, GetRecordsResponse, GoodsItemRecords, Pagination}

import java.time.Instant
import scala.language.postfixOps

trait GetRecordsResponseUtil {
  private val timestamp = Instant.parse("2024-10-12T16:12:34Z")

  def getRecordsResponse(eori: String, page: Int, size: Int): GetRecordsResponse = {
    val totalRecords  = 15
    val effectiveSize = page match {
      case 1 => Math.min(size, 10)
      case 2 => Math.min(size, 5)
      case _ => 0
    }

    val records = if (effectiveSize > 0) {
      (1 to totalRecords).map(_ => getGoodsItemRecords(eori))
    } else {
      Seq.empty[GoodsItemRecords]
    }

    val paginatedRecords = records.slice((page - 1) * size, (page - 1) * size + effectiveSize)

    response.GetRecordsResponse(paginatedRecords, Pagination(totalRecords, page, 2, Some(0), Some(0)))
  }
  def getGoodsItemRecords(eori: String) = GoodsItemRecords(
    eori,
    "GB098765432112",
    "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    "BAN001002",
    "10410100",
    "Not requested",
    "Organic bananas",
    "EC",
    3,
    Some(
      Seq(
        Assessment(
          Some("abc123"),
          Some(1),
          Some(
            Condition(
              Some("abc123"),
              Some("Y923"),
              Some(
                "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"
              ),
              Some("Excluded product")
            )
          )
        )
      )
    ),
    Some(500),
    Some("square meters(m^2)"),
    timestamp,
    Some(Instant.parse("2024-10-12T16:12:34Z")),
    1,
    active = true,
    toReview = false,
    Some("no reason"),
    "IMMI ready",
    "XIUKIM47699357400020231115081800",
    "RMS-GB-123456",
    "6 S12345",
    locked = false,
    timestamp,
    timestamp
  )

}
