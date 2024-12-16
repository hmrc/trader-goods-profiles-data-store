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

import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Assessment, Condition, GoodsItemRecord}

import java.time.Instant

trait GetRecordsResponseUtil {
  private val timestamp = Instant.parse("2024-10-12T16:12:34Z")

  def getTestRecords(eori: String, numRecords: Int): Seq[GoodsItemRecord] =
    (0 until numRecords).map(_ => getGoodsItemRecord(eori))

  def getGoodsItemRecord(eori: String): GoodsItemRecord         = GoodsItemRecord(
    eori,
    "GB098765432112",
    java.util.UUID.randomUUID.toString,
    "BAN001002",
    "10410100",
    "Not requested",
    "Organic bananas",
    "EC",
    Some(3),
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
    "IMMI Ready",
    Some("XIUKIM47699357400020231115081800"),
    Some("RMS-GB-123456"),
    Some("6 S12345"),
    timestamp,
    timestamp
  )
  def getGoodsInactiveItemRecord(eori: String): GoodsItemRecord = GoodsItemRecord(
    eori,
    "GB098765432112",
    java.util.UUID.randomUUID.toString,
    "BAN001002",
    "10410100",
    "Not requested",
    "Organic bananas",
    "EC",
    Some(3),
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
    active = false,
    toReview = false,
    Some("no reason"),
    "IMMI Ready",
    Some("XIUKIM47699357400020231115081800"),
    Some("RMS-GB-123456"),
    Some("6 S12345"),
    timestamp,
    timestamp
  )

}
