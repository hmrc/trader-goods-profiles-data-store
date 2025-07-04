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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.i18n.Lang

import java.time.{Instant, ZoneId}

class DateTimeFormatsSpec extends AnyFreeSpec with Matchers {

  "DateTimeFormats.convertToDateString" - {
    "should format an Instant as 'dd MMMM yyyy' in Europe/London timezone" in {
      val instant = Instant.parse("2024-07-08T10:15:30Z")
      val formatted = DateTimeFormats.convertToDateString(instant)
      formatted shouldBe "08 July 2024"
    }
  }

  "DateTimeFormats.dateTimeFormat" - {
    "should format with English locale by default" in {
      given Lang = Lang("en")
      val formatter = DateTimeFormats.dateTimeFormat()
      val instant   = Instant.parse("2024-12-01T09:00:00Z")
      val londonZoned = instant.atZone(ZoneId.of("Europe/London"))

      formatter.format(londonZoned).toLowerCase shouldBe "1 december 2024 9:00am"
    }

    "should format with Welsh locale if Lang is 'cy'" in {
      given Lang = Lang("cy")
      val formatter = DateTimeFormats.dateTimeFormat()
      val instant   = Instant.parse("2024-12-01T09:00:00Z")
      val londonZoned = instant.atZone(ZoneId.of("Europe/London"))

      formatter.format(londonZoned).toLowerCase shouldBe "1 rhagfyr 2024 9:00yb"
    }

    "should fallback gracefully for unsupported locales" in {
      given Lang = Lang("fr")
      val formatter = DateTimeFormats.dateTimeFormat()
      val instant   = Instant.parse("2024-12-01T09:00:00Z")
      val londonZoned = instant.atZone(ZoneId.of("Europe/London"))

      formatter.format(londonZoned).toLowerCase shouldBe "1 décembre 2024 9:00am"
    }
  }
}
