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
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.{FakeRetireFileAction, FakeStoreLatestAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{RetireFileAction, StoreLatestAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.{DownloadRecordEmailParameters, DownloadRecordEmailRequest}

class EmailConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.email.port" -> wireMockPort)
      .overrides(
        bind[StoreLatestAction].to[FakeStoreLatestAction],
        bind[RetireFileAction].to[FakeRetireFileAction]
      )
      .build()

  private lazy val connector = app.injector.instanceOf[EmailConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".sendDownloadRecordEmail" - {

    val testEmail                         = "someone@somewher.com"
    val testExpiredDate                   = "05 Sept 2024"
    val testDownloadRecordEmailParameters = DownloadRecordEmailParameters(testExpiredDate)
    val testDownloadRecordEmailRequest    = DownloadRecordEmailRequest(Seq(testEmail), testDownloadRecordEmailParameters)

    "must send email2" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/hmrc/email"))
          .withRequestBody(
            equalTo(Json.toJson(testDownloadRecordEmailRequest).toString)
          )
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
          )
      )

      connector.sendDownloadRecordEmail(testEmail, testDownloadRecordEmailParameters).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/hmrc/email"))
          .withRequestBody(
            equalTo(Json.toJson(testDownloadRecordEmailRequest).toString)
          )
          .willReturn(serverError())
      )

      connector.sendDownloadRecordEmail(testEmail, testDownloadRecordEmailParameters).failed.futureValue
    }
  }
}
