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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{CorrelationId, GetRecordsResponse, GoodsItemRecord, Pagination}

import java.time.Instant
import java.util.UUID

class RouterConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with WireMockSupport
    with ScalaFutures
    with IntegrationPatience {

  private lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.trader-goods-profiles-router.port" -> wireMockPort)
      .overrides(
        bind[StoreLatestAction].to[FakeStoreLatestAction]
      )
      .build()

  private lazy val connector = app.injector.instanceOf[RouterConnector]

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val recordId        = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val testEori        = "1122334455"
  private val lastUpdatedDate = Instant.now().toString
  private val eori            = "GB123456789001"
  private val recordsize      = 20
  private val page            = 1

  private val goodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = testEori,
    recordId = recordId,
    traderRef = "BAN001001",
    comcode = "10410100",
    adviceStatus = "Not requested",
    goodsDescription = "Organic bananas",
    countryOfOrigin = "EC",
    category = Some(3),
    assessments = None,
    supplementaryUnit = Some(500),
    measurementUnit = Some("square meters(m^2)"),
    comcodeEffectiveFromDate = Instant.parse("2024-10-12T16:12:34Z"),
    comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
    version = 1,
    active = true,
    toReview = false,
    reviewReason = Some("no reason"),
    declarable = "IMMI ready",
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  private val putRecordRequest: PutRecordRequest = PutRecordRequest(
    actorId = testEori,
    traderRef = "BAN001001",
    comcode = "10410100",
    goodsDescription = "Organic bananas",
    countryOfOrigin = "EC",
    category = Some(3),
    assessments = None,
    supplementaryUnit = Some(500),
    measurementUnit = Some("square meters(m^2)"),
    comcodeEffectiveFromDate = Instant.parse("2024-10-12T16:12:34Z"),
    comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z"))
  )

  ".createTraderProfile" - {

    "must submit a trader profile" in {

      val traderProfile = ProfileRequest(testEori, "1", Some("2"), None)
      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok())
      )

      connector.createTraderProfile(traderProfile, testEori).futureValue
    }

    "must return a failed future when the server returns an error" in {

      val traderProfile = ProfileRequest(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.createTraderProfile(traderProfile, testEori).failed.futureValue
    }
  }

  ".updateTraderProfile" - {

    "must submit a trader profile" in {

      val traderProfile = ProfileRequest(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok())
      )

      connector.updateTraderProfile(traderProfile, testEori).futureValue
    }

    "must return a failed future when the server returns an error" in {

      val traderProfile = ProfileRequest(testEori, "1", Some("2"), None)

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.updateTraderProfile(traderProfile, testEori).failed.futureValue
    }
  }

  ".hasHistoricProfile" - {

    "must return true (profile does exist) when router returns ok" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok())
      )

      connector.hasHistoricProfile(testEori).futureValue mustBe true
    }

    "must return false (profile does not exist) when router returns forbidden" in {
      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(forbidden())
      )

      connector.hasHistoricProfile(testEori).futureValue mustBe false
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.hasHistoricProfile(testEori).failed.futureValue
    }
  }

  ".getRecords" - {

    "must get records from router" in {
      val response = GetRecordsResponse(goodsItemRecords = Seq.empty, Pagination(0, 0, 0, None, None))
      wireMockServer.stubFor(
        get(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$eori/records?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$recordsize"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok().withBody(Json.toJson(response).toString()))
      )

      connector.getRecords(eori, Some(lastUpdatedDate), Some(page), Some(recordsize)).futureValue
    }

    "must get all records from router" in {
      val response = GetRecordsResponse(goodsItemRecords = Seq.empty, Pagination(0, 0, 0, None, None))
      wireMockServer.stubFor(
        get(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$eori/records"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok().withBody(Json.toJson(response).toString()))
      )

      connector.getRecords(eori, None, None, None).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$eori/records?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$recordsize"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.getRecords(eori, Some(lastUpdatedDate), Some(page), Some(recordsize)).failed.futureValue
    }
  }

  ".deleteRecord" - {

    "must delete record from from B&T database" in {

      wireMockServer.stubFor(
        delete(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$eori"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(noContent())
      )

      connector.deleteRecord(eori, recordId).futureValue mustBe true
    }

    "must return false when record is not found" in {

      wireMockServer.stubFor(
        delete(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$eori"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(notFound())
      )

      connector.deleteRecord(eori, recordId).futureValue mustBe false
    }

    "must return false when response is bad request" in {

      wireMockServer.stubFor(
        delete(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$eori"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(badRequest())
      )

      connector.deleteRecord(eori, recordId).futureValue mustBe false
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        delete(
          urlEqualTo(
            s"/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$eori"
          )
        )
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.deleteRecord(eori, recordId).failed.futureValue
    }
  }

  ".getRecord" - {

    "must get a record from B&T database" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok().withBody(Json.toJson(goodsItemRecord).toString()))
      )

      connector.getRecord(testEori, recordId).futureValue mustEqual Some(goodsItemRecord)
    }

    "must return None when not found" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(notFound())
      )

      connector.getRecord(testEori, recordId).futureValue mustEqual None
    }

    "must return None when bad request" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(badRequest())
      )

      connector.getRecord(testEori, recordId).futureValue mustEqual None
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.getRecord(testEori, recordId).failed.futureValue
    }
  }

  ".getRequestDownloadData" - {

    "must request to download data" in {
      val correlationId = UUID.randomUUID().toString
      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori/download"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(status(ACCEPTED).withBody(Json.toJson(CorrelationId(correlationId)).toString()))
      )

      connector.getRequestDownloadData(testEori).futureValue mustEqual CorrelationId(correlationId)
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        get(urlEqualTo(s"/trader-goods-profiles-router/customs/traders/goods-profiles/$testEori/download"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.getRequestDownloadData(testEori).failed.futureValue
    }
  }

  ".patchRecord" - {

    "must update a record in B&T database" in {

      val updateRecord = PatchRecordRequest(testEori, Some("updated-trader-ref"))

      wireMockServer.stubFor(
        patch(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok())
      )

      connector.patchRecord(updateRecord, testEori, recordId).futureValue mustBe true
    }

    "must return false when not found" in {

      val updateRecord = PatchRecordRequest(testEori, Some("updated-trader-ref"))

      wireMockServer.stubFor(
        patch(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(notFound())
      )

      connector.patchRecord(updateRecord, testEori, recordId).futureValue mustBe false
    }

    "must return a failed future when the server returns an error" in {

      val updateRecord = PatchRecordRequest(testEori, Some("updated-trader-ref"))

      wireMockServer.stubFor(
        patch(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.patchRecord(updateRecord, testEori, recordId).failed.futureValue
    }
  }

  ".putRecord" - {

    "must update a record in B&T database" in {

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(ok())
      )

      connector.putRecord(putRecordRequest, testEori, recordId).futureValue mustBe true
    }

    "must return false when not found" in {

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(notFound())
      )

      connector.putRecord(putRecordRequest, testEori, recordId).futureValue mustBe false
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.putRecord(putRecordRequest, testEori, recordId).failed.futureValue
    }
  }

  ".createRecord" - {

    "must create a record in B&T database" in {

      val createRecordRequest =
        CreateRecordRequest(
          testEori,
          testEori,
          "BAN001001",
          "10410100",
          "Organic bananas",
          "EC",
          Instant.parse("2024-10-12T16:12:34Z"),
          comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
          Some(3)
        )

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(created().withBody(Json.toJson(goodsItemRecord).toString()))
      )

      connector.createRecord(createRecordRequest, testEori).futureValue mustEqual goodsItemRecord
    }

    "must return a failed future when the server returns an error" in {

      val createRecordRequest =
        CreateRecordRequest(testEori, testEori, "test", "test", "test", "test", Instant.now, Some(Instant.now), Some(1))

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.createRecord(createRecordRequest, testEori).failed.futureValue
    }
  }

  ".withdrawAdvice" - {

    val withdrawReason = WithdrawReasonRequest(Some("REASON"))

    "must withdraw advice and return no content" in {

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId/advice"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(noContent())
      )

      connector.withdrawAdvice(testEori, recordId, withdrawReason).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        put(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId/advice"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.withdrawAdvice(testEori, recordId, withdrawReason).failed.futureValue
    }
  }

  ".requestAdvice" - {

    val advice = AdviceRequest(testEori, "TESTNAME", testEori, recordId, "TEST@email.com")

    "must request advice successfully" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId/advice"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(created())
      )

      connector.requestAdvice(testEori, recordId, advice).futureValue
    }

    "must return a failed future when the server returns an error" in {

      wireMockServer.stubFor(
        post(urlEqualTo(s"/trader-goods-profiles-router/traders/$testEori/records/$recordId/advice"))
          .withHeader("X-Client-ID", equalTo("tgp-frontend"))
          .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
          .willReturn(serverError())
      )

      connector.requestAdvice(testEori, recordId, advice).failed.futureValue
    }
  }
}
