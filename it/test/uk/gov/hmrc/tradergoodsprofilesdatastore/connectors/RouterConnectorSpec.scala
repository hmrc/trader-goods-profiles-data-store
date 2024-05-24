package uk.gov.hmrc.tradergoodsprofilesdatastore.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.ProfileRequest
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport

class RouterConnectorSpec
  extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-router.port" -> wireMockPort)
      .build()

  private lazy val connector = app.injector.instanceOf[RouterConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val testEori = "1122334455"

  ".submitTraderProfile" - {

    "must submit a trader profile" in {

      val traderProfile = ProfileRequest(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/good-profiles/$testEori"))
          .withRequestBody(equalTo(Json.toJson(traderProfile).toString))
          .willReturn(ok())
      )

      connector.submitTraderProfile(traderProfile, testEori).futureValue
    }

    "must return a failed future when the server returns an error" in {

      val traderProfile = ProfileRequest(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo("/trader-goods-profiles-router/customs/traders/good-profiles"))
          .withRequestBody(equalTo(Json.toJson(traderProfile).toString))
          .willReturn(serverError())
      )

      connector.submitTraderProfile(traderProfile, testEori).failed.futureValue
    }
  }
}
