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

import play.api.i18n.Lang

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import java.util.Locale

object DateTimeFormats {

  private val baseDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy h:mma")

  private def localisedFormatter(lang: Lang): DateTimeFormatter = {
    val locale = Locale.forLanguageTag(lang.code)
    baseDateTimeFormatter.withLocale(locale)
  }

  def dateTimeFormat()(implicit lang: Lang): DateTimeFormatter =
    localisedFormatter(lang)

  def convertToDateString(date: Instant): String =
    DateTimeFormatter
      .ofPattern("dd MMMM yyyy")
      .withZone(ZoneId.of("Europe/London"))
      .format(date)
}
