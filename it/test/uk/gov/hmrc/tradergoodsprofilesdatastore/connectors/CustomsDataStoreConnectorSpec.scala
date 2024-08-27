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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.ACCEPTED
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeStoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.StoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests._
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Email, GetRecordsResponse, GoodsItemRecord, Pagination}

import java.time.Instant

class CustomsDataStoreConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.customs-data-store.port" -> wireMockPort)
      .overrides(
        bind[StoreLatestAction].to[FakeStoreLatestAction]
      )
      .build()

  private lazy val connector = app.injector.instanceOf[CustomsDataStoreConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val testEori = "1122334455"

  ".getEmail" - {

    "must get email" in {

      val address   = "email@address.co.uk"
      val timestamp = Instant.now

      val email = Email(address, timestamp)

      wireMockServer.stubFor(
        get(urlEqualTo(s"/customs-data-store/eori/$testEori/verified-email"))
          .withHeader("Authorization", equalTo("secret-token"))
          .willReturn(ok().withBody(Json.toJson(email).toString()))
      )

      connector.getEmail(testEori).futureValue mustEqual Some(email)
    }

    "must return None if not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/customs-data-store/eori/$testEori/verified-email"))
          .withHeader("Authorization", equalTo("secret-token"))
          .willReturn(notFound())
      )

      connector.getEmail(testEori).futureValue mustEqual None
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/customs-data-store/eori/$testEori/verified-email"))
          .withHeader("Authorization", equalTo("secret-token"))
          .willReturn(serverError())
      )

      connector.getEmail(testEori).failed.futureValue
    }
  }
}
