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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.BeforeAndAfterEach
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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Email, EoriHistoricItem, EoriHistoryResponse}
import uk.gov.hmrc.http.Authorization
import java.time.{Instant, LocalDate}

class CustomsDataStoreConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach {

  private def app(cdsMigration: Boolean = false): Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.customs-data-store.port" -> wireMockPort)
      .configure("features.stub-verified-email" -> false)
      .configure("features.cds-migration" -> cdsMigration)
      .overrides(
        bind[StoreLatestAction].to[FakeStoreLatestAction]
      )
      .build()

  private lazy val connector = app(cdsMigration = false).injector.instanceOf[CustomsDataStoreConnector]
  private lazy val connectorWithMigration = app(cdsMigration = true).injector.instanceOf[CustomsDataStoreConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val testEori = "1122334455"

  ".getEmailViaPost" - {

    "must get email" in {

      val address = "email@address.co.uk"
      val timestamp = Instant.now

      val email = Email(address, timestamp)

      wireMockServer.stubFor(
        post(urlEqualTo(s"/customs-data-store/eori/verified-email-third-party"))
          .willReturn(ok().withBody(Json.toJson(email).toString()))
      )

      connectorWithMigration.getEmailViaPost(testEori).futureValue mustBe Some(email)
    }

    "must return None if not found" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/customs-data-store/eori/verified-email-third-party"))
          .willReturn(notFound())
      )

      connectorWithMigration.getEmailViaPost(testEori).futureValue mustBe None
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/customs-data-store/eori/verified-email-third-party"))
          .willReturn(serverError())
      )

      connectorWithMigration.getEmailViaPost(testEori).failed.futureValue
    }
  }


  ".getEmail" - {

    "must get email" in {

      val address   = "email@address.co.uk"
      val timestamp = Instant.now

      val email = Email(address, timestamp)

      wireMockServer.stubFor(
        get(urlEqualTo(s"/customs-data-store/eori/$testEori/verified-email"))
          .willReturn(ok().withBody(Json.toJson(email).toString()))
      )

      connector.getEmail(testEori).futureValue mustBe Some(email)
    }

    "must return None if not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/customs-data-store/eori/$testEori/verified-email"))
          .willReturn(notFound())
      )

      connector.getEmail(testEori).futureValue mustBe None
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/customs-data-store/eori/$testEori/verified-email"))
          .willReturn(serverError())
      )

      connector.getEmail(testEori).failed.futureValue
    }
  }

  ".getEoriHistory" - {

    "when authorisation token is Some()" - {

      val authToken = Authorization("some-token")

      "must return eori history" in {

        val mockEoriHistoryResponse = EoriHistoryResponse(
          Seq(
            EoriHistoricItem("eori1", LocalDate.parse("2024-02-20"), Some(LocalDate.parse("2024-01-20"))),
            EoriHistoricItem("eori1", LocalDate.parse("2024-03-20"), Some(LocalDate.parse("2024-01-20"))),
            EoriHistoricItem("eori1", LocalDate.parse("2024-01-20"), Some(LocalDate.parse("2024-01-20")))
          )
        )

        wireMockServer.stubFor(
          get(urlEqualTo(s"/customs-data-store/eori/eori-history"))
            .willReturn(ok().withBody(Json.toJson(mockEoriHistoryResponse).toString()))
        )

        val expectedResponse = EoriHistoryResponse(
          Seq(
            EoriHistoricItem("eori1", LocalDate.parse("2024-03-20"), Some(LocalDate.parse("2024-01-20"))),
            EoriHistoricItem("eori1", LocalDate.parse("2024-02-20"), Some(LocalDate.parse("2024-01-20"))),
            EoriHistoricItem("eori1", LocalDate.parse("2024-01-20"), Some(LocalDate.parse("2024-01-20")))
          )
        )

        connectorWithMigration.getEoriHistory(testEori, Some(authToken)).futureValue mustBe Some(expectedResponse)

        wireMockServer.verify(
          1,
          getRequestedFor(urlEqualTo(s"/customs-data-store/eori/eori-history"))
            .withHeader("Authorization", equalTo(s"Bearer ${authToken.value}"))
        )
      }

      "must return None if not found" in {

        wireMockServer.stubFor(
          get(urlEqualTo(s"/customs-data-store/eori/eori-history"))
            .willReturn(notFound())
        )

        connectorWithMigration.getEoriHistory(testEori, Some(authToken)).futureValue mustBe None

        wireMockServer.verify(
          1,
          getRequestedFor(urlEqualTo(s"/customs-data-store/eori/eori-history"))
            .withHeader("Authorization", equalTo(s"Bearer ${authToken.value}"))
        )
      }

      "must return a failed future when the server returns an error" in {
        wireMockServer.stubFor(
          get(urlEqualTo(s"/customs-data-store/eori/eori-history"))
            .willReturn(serverError())
        )

        connectorWithMigration.getEoriHistory(testEori, Some(authToken)).failed.futureValue

        wireMockServer.verify(
          1,
          getRequestedFor(urlEqualTo(s"/customs-data-store/eori/eori-history"))
            .withHeader("Authorization", equalTo(s"Bearer ${authToken.value}"))
        )
      }
    }

    "when authorisation token is None" - {

      "must return eori history" in {

        val mockEoriHistoryResponse = EoriHistoryResponse(
          Seq(
            EoriHistoricItem("eori1", LocalDate.parse("2024-02-20"), Some(LocalDate.parse("2024-01-20"))),
            EoriHistoricItem("eori1", LocalDate.parse("2024-03-20"), Some(LocalDate.parse("2024-01-20"))),
            EoriHistoricItem("eori1", LocalDate.parse("2024-01-20"), Some(LocalDate.parse("2024-01-20")))
          )
        )
        wireMockServer.stubFor(
          get(urlEqualTo(s"/customs-data-store/eori/$testEori/eori-history"))
            .willReturn(ok().withBody(Json.toJson(mockEoriHistoryResponse).toString()))
        )

        val expectedResponse = EoriHistoryResponse(
          Seq(
            EoriHistoricItem("eori1", LocalDate.parse("2024-03-20"), Some(LocalDate.parse("2024-01-20"))),
            EoriHistoricItem("eori1", LocalDate.parse("2024-02-20"), Some(LocalDate.parse("2024-01-20"))),
            EoriHistoricItem("eori1", LocalDate.parse("2024-01-20"), Some(LocalDate.parse("2024-01-20")))
          )
        )

        connector.getEoriHistory(testEori).futureValue mustBe Some(expectedResponse)
      }

      "must return None if not found" in {

        wireMockServer.stubFor(
          get(urlEqualTo(s"/customs-data-store/eori/$testEori/eori-history"))
            .willReturn(notFound())
        )

        connector.getEoriHistory(testEori).futureValue mustBe None
      }

      "must return a failed future when the server returns an error" in {
        wireMockServer.stubFor(
          get(urlEqualTo(s"/customs-data-store/eori/$testEori/eori-history"))
            .willReturn(serverError())
        )

        connector.getEoriHistory(testEori).failed.futureValue
      }
    }
  }
}
