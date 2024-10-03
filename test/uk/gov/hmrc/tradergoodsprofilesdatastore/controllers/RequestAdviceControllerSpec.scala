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
import org.mockito.Mockito.{verify, when}
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
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.AdviceRequest

import scala.concurrent.{ExecutionContext, Future}

class RequestAdviceControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global
  private val testEori              = "GB123456789099"
  private val testRecordId          = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val advice = AdviceRequest(testEori, "TESTNAME", testEori, testRecordId, "TEST@email.com")

  private lazy val requestUrl = routes.RequestAdviceController.requestAdvice(testEori, testRecordId).url

  s"POST $requestUrl" - {

    "return 201 when advice is successfully created" in {

      val mockRouterConnector = mock[RouterConnector]
      when(
        mockRouterConnector.requestAdvice(any(), any(), any())(any())
      ) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {

        val request = FakeRequest(routes.RequestAdviceController.requestAdvice(testEori, testRecordId))
          .withHeaders("Content-Type" -> "application/json")
          .withJsonBody(Json.toJson(advice))

        val result = route(application, request).value
        status(result) shouldBe CREATED

        withClue("must call the relevant services with the correct details") {
          verify(mockRouterConnector)
            .requestAdvice(eqTo(testEori), eqTo(testRecordId), eqTo(advice))(any())
        }
      }
    }
  }
}
