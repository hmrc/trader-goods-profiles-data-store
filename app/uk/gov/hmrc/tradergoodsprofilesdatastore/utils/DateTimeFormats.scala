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

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormats {

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  private val localisedDateTimeFormatters = Map(
    "en" -> dateTimeFormatter,
    "cy" -> dateTimeFormatter.withLocale(new Locale("cy"))
  )

  def dateTimeFormat(langCode: String): DateTimeFormatter =
    localisedDateTimeFormatters.getOrElse(langCode, dateTimeFormatter)

  val dateTimeHintFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d M yyyy")

  def convertToDateString(instant: Instant, isWelsh: Boolean): String =
    instant
      .atZone(ZoneOffset.UTC)
      .toLocalDate
      .format(dateTimeFormat(if (isWelsh) {
        "cy"
      } else {
        "en"
      }))
}
