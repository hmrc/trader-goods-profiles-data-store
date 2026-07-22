/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofilesdatastore.config

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration

class ServiceSpec extends AnyFreeSpec with Matchers {

  "Service" - {

    "must build a base url" in {
      val service = Service("localhost", "8080", "http")

      service.baseUrl mustBe "http://localhost:8080"
      service.toString mustBe "http://localhost:8080"
    }

    "must convert implicitly to a string" in {
      val service          = Service("localhost", "8080", "http")
      val asString: String = service

      asString mustBe "http://localhost:8080"
    }

    "must load from config" in {
      val config = Configuration(
        "service.host"     -> "localhost",
        "service.port"     -> "8080",
        "service.protocol" -> "http"
      )

      config.get[Service]("service") mustBe Service("localhost", "8080", "http")
    }
  }
}
