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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, verify, when}

import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeIdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Assessment, Condition, GoodsItemRecord}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class DeleteRecordControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val testEori             = "eori"
  private val testRecordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val getUrl = routes.DeleteRecordController.deleteRecord(testRecordId).url

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
    category = Some(3),
    assessments = Some(Seq(sampleAssessment)),
    supplementaryUnit = Some(500),
    measurementUnit = Some("square meters(m^2)"),
    comcodeEffectiveFromDate = Instant.parse("2024-10-12T16:12:34Z"),
    comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
    version = 1,
    active = true,
    toReview = false,
    reviewReason = Some("no reason"),
    declarable = "IMMI Ready",
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
    category = Some(3),
    assessments = Some(Seq(sampleAssessment)),
    supplementaryUnit = Some(500),
    measurementUnit = Some("square meters(m^2)"),
    comcodeEffectiveFromDate = Instant.parse("2024-10-12T16:12:34Z"),
    comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
    version = 1,
    active = false,
    toReview = false,
    reviewReason = Some("no reason"),
    declarable = "IMMI Ready",
    ukimsNumber = Some("XIUKIM47699357400020231115081800"),
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345"),
    createdDateTime = Instant.parse("2024-10-12T16:12:34Z"),
    updatedDateTime = Instant.parse("2024-10-12T16:12:34Z")
  )

  s"DELETE $getUrl" - {

    "return 204 when record is deleted" in {

      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.deleteRecord(any(), any())(any())
      ).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe NO_CONTENT

        verify(mockRouterConnector, atLeastOnce())
          .deleteRecord(any(), any())(any())
      }
    }

    "return 404 when record does not exist or is inactive" in {

      val mockRouterConnector = mock[RouterConnector]

      when(
        mockRouterConnector.deleteRecord(any(), any())(any())
      ).thenReturn(Future.successful(false))

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) mustBe NOT_FOUND

        verify(mockRouterConnector, atLeastOnce())
          .deleteRecord(any(), any())(any())
      }

    }
  }
}
