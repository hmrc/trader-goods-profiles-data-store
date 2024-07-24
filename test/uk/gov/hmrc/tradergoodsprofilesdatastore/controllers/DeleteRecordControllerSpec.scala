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

package uk.gov.hmrc.tradergoodsprofilesdatastore.controllers

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeIdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Assessment, Condition, GoodsItemRecord}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class DeleteRecordControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val testEori             = "GB123456789099"
  private val testRecordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val getUrl = routes.DeleteRecordController.deleteRecord(testEori, testRecordId).url

  private val validFakeGetRequest = FakeRequest("DELETE", getUrl)

  val sampleCondition: Condition = Condition(
    `type` = Some("abc123"),
    conditionId = Some("Y923"),
    conditionDescription =
      Some("Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"),
    conditionTraderText = Some("Excluded product")
  )

  val sampleAssessment: Assessment = Assessment(
    assessmentId = Some("abc123"),
    primaryCategory = Some(1),
    condition = Some(sampleCondition)
  )

  val sampleGoodsItemRecord: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = testRecordId,
    traderRef = "BAN001001",
    comcode = "10410100",
    adviceStatus = "Not requested",
    goodsDescription = "Organic bananas",
    countryOfOrigin = "EC",
    category = 3,
    assessments = Some(Seq(sampleAssessment)),
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

  val sampleInactiveGoodsItemRecord: GoodsItemRecord = GoodsItemRecord(
    eori = testEori,
    actorId = "GB098765432112",
    recordId = testRecordId,
    traderRef = "BAN001001",
    comcode = "10410100",
    adviceStatus = "Not requested",
    goodsDescription = "Organic bananas",
    countryOfOrigin = "EC",
    category = 3,
    assessments = Some(Seq(sampleAssessment)),
    supplementaryUnit = Some(500),
    measurementUnit = Some("square meters(m^2)"),
    comcodeEffectiveFromDate = Instant.parse("2024-10-12T16:12:34Z"),
    comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
    version = 1,
    active = false,
    toReview = false,
    reviewReason = Some("no reason"),
    declarable = "IMMI ready",
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  s"DELETE $getUrl" - {

    "return 204 when record is deleted from data store" in {

      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.deleteRecord(any(), any())(any())
      ) thenReturn Future.successful(Done)

      when(
        mockRouterConnector.getRecord(any(), any())(any())
      ) thenReturn Future.successful(sampleGoodsItemRecord)

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe NO_CONTENT

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector)
            .deleteRecord(eqTo(testEori), eqTo(testRecordId))(any())
          verify(mockRouterConnector)
            .getRecord(eqTo(testEori), eqTo(testRecordId))(any())
        }
      }
    }

    "return 404 when record does not exist because it has never existed (400)" in {

      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.deleteRecord(any(), any())(any())
      ) thenReturn Future.successful(Done)

      when(
        mockRouterConnector.getRecord(any(), any())(any())
      ) thenReturn Future.failed(UpstreamErrorResponse("could not get", BAD_REQUEST))

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe NOT_FOUND

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector, never())
            .deleteRecord(eqTo(testEori), eqTo(testRecordId))(any())
          verify(mockRouterConnector)
            .getRecord(eqTo(testEori), eqTo(testRecordId))(any())
        }
      }
    }

    "return 404 when record does not exist because it has never existed (404)" in {

      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.deleteRecord(any(), any())(any())
      ) thenReturn Future.successful(Done)

      when(
        mockRouterConnector.getRecord(any(), any())(any())
      ) thenReturn Future.failed(UpstreamErrorResponse("could not get", NOT_FOUND))

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe NOT_FOUND

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector, never())
            .deleteRecord(eqTo(testEori), eqTo(testRecordId))(any())
          verify(mockRouterConnector)
            .getRecord(eqTo(testEori), eqTo(testRecordId))(any())
        }
      }
    }

    "return 404 when record does not exist because it is now inactive" in {

      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.deleteRecord(any(), any())(any())
      ) thenReturn Future.successful(Done)

      when(
        mockRouterConnector.getRecord(any(), any())(any())
      ) thenReturn Future.successful(sampleInactiveGoodsItemRecord)

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe NOT_FOUND

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector, never())
            .deleteRecord(eqTo(testEori), eqTo(testRecordId))(any())
          verify(mockRouterConnector)
            .getRecord(eqTo(testEori), eqTo(testRecordId))(any())
        }
      }
    }

    "return 500 when there is an error when deleting on the router" in {
      val mockRouterConnector = mock[RouterConnector]
      when(
        mockRouterConnector.deleteRecord(any(), any())(any())
      ) thenReturn Future.failed(UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR))

      when(
        mockRouterConnector.getRecord(any(), any())(any())
      ) thenReturn Future.successful(sampleGoodsItemRecord)

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe INTERNAL_SERVER_ERROR

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector)
            .deleteRecord(eqTo(testEori), eqTo(testRecordId))(any())
          verify(mockRouterConnector)
            .getRecord(eqTo(testEori), eqTo(testRecordId))(any())
        }
      }
    }

    "return 500 when there is an error when getting the record" in {
      val mockRouterConnector = mock[RouterConnector]
      when(
        mockRouterConnector.deleteRecord(any(), any())(any())
      ) thenReturn Future.successful(Done)

      when(
        mockRouterConnector.getRecord(any(), any())(any())
      ) thenReturn Future.failed(UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR))

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe INTERNAL_SERVER_ERROR

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector, never())
            .deleteRecord(eqTo(testEori), eqTo(testRecordId))(any())
          verify(mockRouterConnector)
            .getRecord(eqTo(testEori), eqTo(testRecordId))(any())
        }
      }
    }
  }
}
