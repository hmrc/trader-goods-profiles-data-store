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
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeStoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.StoreLatestAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{DownloadData, Metadata}

class SecureDataExchangeProxyConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.secure-data-exchange-proxy.port" -> wireMockPort)
      .overrides(
        bind[StoreLatestAction].to[FakeStoreLatestAction]
      )
      .build()

  private lazy val connector = app.injector.instanceOf[SecureDataExchangeProxyConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val testEori        = "1122334455"
  private val informationType = "1722779512141"
  private val serverToken     = "ygR8dU8knJORDqRqiAgTVhGoRoDi"

  ".getFilesAvailable" - {

    "must get file list" in {

      val url                      = "/some-url"
      val filename                 = "filename"
      val filesize                 = 600
      val fileRoleMetadata         = Metadata("FileRole", "C79Certificate")
      val periodStartYearMetadata  = Metadata("PeriodStartYear", "2020")
      val retentionDaysMetadata    = Metadata("RETENTION_DAYS", "217")
      val periodStartMonthMetadata = Metadata("PeriodStartMonth", "08")

      val downloadData = DownloadData(
        url,
        filename,
        filesize,
        Seq(
          fileRoleMetadata,
          periodStartYearMetadata,
          retentionDaysMetadata,
          periodStartMonthMetadata
        )
      )

      wireMockServer.stubFor(
        get(urlEqualTo(s"/secure-data-exchange-proxy/files-available/list/$informationType"))
          .withHeader("x-client-id", equalTo(serverToken))
          .withHeader("X-SDES-Key", equalTo(testEori))
          .willReturn(ok().withBody(Json.toJson(Seq(downloadData)).toString()))
      )

      connector.getFilesAvailableUrl(testEori).futureValue mustEqual Seq(downloadData)
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/secure-data-exchange-proxy/files-available/list/$informationType"))
          .withHeader("x-client-id", equalTo(serverToken))
          .withHeader("X-SDES-Key", equalTo(testEori))
          .willReturn(serverError())
      )

      connector.getFilesAvailableUrl(testEori).failed.futureValue
    }
  }
}
