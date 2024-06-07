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

package uk.gov.hmrc.tradergoodsprofilesdatastore.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.ProfileRequest
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

import java.time.Instant

class RouterConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-stubs.port" -> wireMockPort)
      .configure("microservice.services.trader-goods-profiles-router.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[RouterConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val testEori        = "1122334455"
  private val lastUpdatedDate = Instant.now().toString
  private val eori            = "GB123456789001"
  private val pagesize        = 1
  private val page            = 1

  ".submitTraderProfile" - {

    "must submit a trader profile" in {

      val traderProfile = ProfileRequest(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .willReturn(ok())
      )

      connector.submitTraderProfile(traderProfile, testEori).futureValue
    }

    "must return a failed future when the server returns an error" in {

      val traderProfile = ProfileRequest(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .willReturn(serverError())
      )

      connector.submitTraderProfile(traderProfile, testEori).failed.futureValue
    }
  }

  ".getTGPRecords" - {

    "must get records from router" in {

      wireMockServer.stubFor(
        get(
          urlEqualTo(
            s"/trader-goods-profiles-router/$eori?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$pagesize"
          )
        )
          .willReturn(ok())
      )

      connector.getRecords(eori, Some(lastUpdatedDate), Some(page), Some(pagesize)).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(
          urlEqualTo(s"/trader-goods-profiles-router/$eori?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$pagesize")
        )
          .willReturn(serverError())
      )

      connector.getRecords(eori, Some(lastUpdatedDate), Some(page), Some(pagesize)).failed.futureValue
    }
  }
}
