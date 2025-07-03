/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofilesdatastore.services

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.RecordsSummary.Update
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, GoodsItemRecord, Pagination}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{RecordsRepository, RecordsSummaryRepository}
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.PaginationHelper

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
class StoreRecordsServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val mockRouter           = mock[RouterConnector]
  private val mockRepo             = mock[RecordsRepository]
  private val mockSummary          = mock[RecordsSummaryRepository]
  private val now                  = Instant.now().plus(1, ChronoUnit.DAYS)
  private val clock                = Clock.fixed(now, ZoneOffset.UTC)
  private val mockPaginationHelper = mock[PaginationHelper]

  private val pageSize     = 2
  private val startingPage = 0

  when(mockPaginationHelper.localPageSize).thenReturn(pageSize)
  when(mockPaginationHelper.localStartingPage).thenReturn(startingPage)

  private val service = new StoreRecordsService(
    mockRouter,
    mockRepo,
    mockSummary,
    clock,
    mockPaginationHelper
  )(ec)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRouter, mockRepo, mockSummary, mockPaginationHelper)
    when(mockPaginationHelper.localPageSize).thenReturn(pageSize)
    when(mockPaginationHelper.localStartingPage).thenReturn(startingPage)

    when(mockSummary.set(any[String], any[Option[Update]], any[Instant]))
      .thenReturn(Future.successful(Done))
    when(mockSummary.update(any[String], any[Option[Update]], any[Option[Instant]]))
      .thenReturn(Future.successful(Done))
  }

  private def makeRecord(eori: String, daysAgo: Long): GoodsItemRecord = {
    val base = now.minus(daysAgo, ChronoUnit.DAYS)
    GoodsItemRecord(
      recordId = s"rec-$daysAgo",
      eori = eori,
      actorId = "actor123",
      traderRef = "TRADERREF",
      comcode = "0101210000",
      adviceStatus = "PENDING",
      goodsDescription = "Some goods",
      countryOfOrigin = "GB",
      category = None,
      assessments = None,
      supplementaryUnit = None,
      measurementUnit = None,
      comcodeEffectiveFromDate = base,
      comcodeEffectiveToDate = None,
      version = 1,
      active = true,
      toReview = false,
      reviewReason = None,
      declarable = "Y",
      ukimsNumber = None,
      nirmsNumber = None,
      niphlNumber = None,
      createdDateTime = base,
      updatedDateTime = base
    )
  }

  "StoreRecordsService.storeRecords" should {

    "return true and update only summary to (0,0) when no records are returned" in {
      val eori = "GB000000000001"

      when(mockRouter.getRecords(eqTo(eori), any(), any(), any())(any()))
        .thenReturn(Future.successful(GetRecordsResponse(Seq.empty, Pagination(0, startingPage, pageSize, None, None))))
      when(mockSummary.set(any(), any(), any())).thenReturn(Future.successful(Done))

      val result = await(service.storeRecords(eori, None))
      result mustBe true

      verify(mockRepo, never).updateRecords(any(), any())
      verify(mockSummary, times(1)).set(eqTo(eori), eqTo(Some(Update(0, 0))), any())
    }

    "should fetch three pages (two nonempty + one empty) and update only the nonempty pages" in {
      val eori  = "GB000000000003"
      val page0 = Seq(makeRecord(eori, 3), makeRecord(eori, 2))
      val page1 = Seq(makeRecord(eori, 1))
      val page2 = Seq.empty[GoodsItemRecord]
      val total = page0.size + page1.size // total = 3

      when(mockRouter.getRecords(eqTo(eori), any(), any(), any())(any()))
        .thenReturn(
          Future.successful(GetRecordsResponse(page0, Pagination(total, 0, pageSize, Some(1), None))),
          Future.successful(GetRecordsResponse(page1, Pagination(total, 1, pageSize, Some(2), None))),
          Future.successful(GetRecordsResponse(page2, Pagination(total, 2, pageSize, None, None)))
        )

      when(mockRepo.updateRecords(eqTo(eori), any())).thenReturn(Future.successful(Done))

      when(mockSummary.set(eqTo(eori), any(), any())).thenReturn(Future.successful(Done))

      await(service.storeRecords(eori, None))

      verify(mockRepo, times(2)).updateRecords(eqTo(eori), any())

      eventually {
        noException should be thrownBy {
          verify(mockSummary, org.mockito.Mockito.atLeast(3)).set(eqTo(eori), any(), any())
        }
      }
    }

    "handle failure on second page by clearing summary and returning false" in {
      val eori        = "GB000000000004"
      val page0       = Seq(makeRecord(eori, 1))
      val totalPages0 = page0.size + 1

      when(mockRouter.getRecords(eqTo(eori), any(), any(), any())(any()))
        .thenReturn(
          Future.successful(GetRecordsResponse(page0, Pagination(totalPages0, 0, pageSize, Some(1), None))),
          Future.failed(new RuntimeException("boom"))
        )

      when(mockRepo.updateRecords(eqTo(eori), eqTo(page0))).thenReturn(Future.successful(Done))
      when(mockSummary.set(any(), any(), any())).thenReturn(Future.successful(Done))
      when(mockSummary.update(eqTo(eori), eqTo(None), eqTo(None))).thenReturn(Future.successful(Done))

      val result = await(service.storeRecords(eori, None))
      result mustBe false

      verify(mockRepo, times(1)).updateRecords(eqTo(eori), eqTo(page0))
      verify(mockSummary, times(1)).set(eqTo(eori), eqTo(Some(Update(page0.size, totalPages0))), any())
      verify(mockSummary, times(1)).update(eqTo(eori), eqTo(None), eqTo(None))
    }
  }
}
