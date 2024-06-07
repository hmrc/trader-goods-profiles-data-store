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
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val requestEori             = "GB123456789099"
  private val lastUpdatedDate = Instant.now().toString
  private val pagesize        = 1
  private val page            = 1

  private val getUrl              = routes.GetRecordsController
    .getRecords(requestEori, Some(lastUpdatedDate), Some(page), Some(pagesize))
    .url
  private val validFakeGetRequest = FakeRequest(
    "GET",
    getUrl
  )

  s"GET $getUrl" - {

    "return 200 and the records when data is successfully retrieved and saved" in {

      val recordsResponse       = getRecordsResponse(requestEori, page, pagesize)
      val httpResponse          = HttpResponse(OK, Json.toJson(recordsResponse).toString())
      val mockRecordsRepository = mock[RecordsRepository]
      when(mockRecordsRepository.saveRecords(any())) thenReturn Future.successful(true)

      val mockRouterConnector = mock[RouterConnector]
      when(mockRouterConnector.getRecords(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(httpResponse))

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository),
          bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()
      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK
        contentAsString(result) mustBe Json.toJson(recordsResponse).toString
      }
    }

    "return 500 when saving in repository fails" in {
      val recordsResponse       = getRecordsResponse(requestEori, page, pagesize)
      val httpResponse          = HttpResponse(OK, Json.toJson(recordsResponse).toString())
      val mockRecordsRepository = mock[RecordsRepository]
      val mockRouterConnector   = mock[RouterConnector]
      when(mockRouterConnector.getRecords(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(httpResponse))
      when(mockRecordsRepository.saveRecords(any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val application = applicationBuilder()
        .overrides(
          bind[RecordsRepository].toInstance(mockRecordsRepository),
          bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return the appropriate status code when the router connector returns an error" in {
      val httpResponse = HttpResponse(BAD_REQUEST, "Bad Request")

      val mockRouterConnector = mock[RouterConnector]
      when(mockRouterConnector.getRecords(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(httpResponse))

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return 500 when the router connector throws an exception" in {
      val mockRouterConnector = mock[RouterConnector]
      when(mockRouterConnector.getRecords(any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException()))

      val application = applicationBuilder()
        .overrides(
          bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
