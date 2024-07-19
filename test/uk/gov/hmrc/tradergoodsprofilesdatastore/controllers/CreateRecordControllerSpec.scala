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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeIdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.CreateRecordRequest

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CreateRecordControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global
  val testEori                      = "GB123456789099"

  private val goodsRecord  =
    CreateRecordRequest(
      testEori,
      testEori,
      "BAN001001",
      "10410100",
      "Organic bananas",
      "EC",
      Instant.parse("2024-10-12T16:12:34Z"),
      comcodeEffectiveToDate = Some(Instant.parse("2024-10-12T16:12:34Z")),
      3
    )
  private val testRecordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val createUrl = routes.CreateRecordController.createRecord(testEori).url

  private val validFakeCreateRequest = FakeRequest("POST", createUrl)

  s"POST $createUrl" - {

    "return 200 when record is successfully created" in {

      val mockRouterConnector = mock[RouterConnector]
      when(
        mockRouterConnector.createRecord(any(), any())(any())
      ) thenReturn Future.successful(testRecordId)

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()
      running(application) {
        val request = validFakeCreateRequest
          .withHeaders("Content-Type" -> "application/json")
          .withJsonBody(Json.toJson(goodsRecord))
        val result  = route(application, request).value
        status(result) shouldBe OK

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector, times(1))
            .createRecord(eqTo(goodsRecord), eqTo(testEori))(any())
        }
      }
    }
  }
}
