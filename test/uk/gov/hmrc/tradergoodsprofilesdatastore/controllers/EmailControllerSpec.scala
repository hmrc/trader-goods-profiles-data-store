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
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeIdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Email
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.GetRecordsResponseUtil

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class EmailControllerSpec extends SpecBase with MockitoSugar with GetRecordsResponseUtil {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "getEmail" - {

    "return 200 with email in body when it is found" in {
      val requestEori = "GB123456789099"
      val timestamp   = Instant.now
      val address     = "email@test.co.uk"
      val email       = Email(address, timestamp)
      val getEmailUrl = routes.EmailController
        .getEmail(requestEori)
        .url

      val validFakeGetRequest = FakeRequest("GET", getEmailUrl)

      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]

      when(mockCustomsDataStoreConnector.getEmail(any())(any())) thenReturn Future.successful(Some(email))

      val application = applicationBuilder()
        .overrides(
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK
        contentAsString(result) mustBe Json.toJson(email).toString
      }
      verify(mockCustomsDataStoreConnector, atLeastOnce()).getEmail(any())(any())
    }

    "return 404 when email does not exist" in {
      val requestEori = "GB123456789099"
      val getEmailUrl = routes.EmailController
        .getEmail(requestEori)
        .url

      val validFakeGetRequest = FakeRequest("GET", getEmailUrl)

      val mockCustomsDataStoreConnector = mock[CustomsDataStoreConnector]

      when(
        mockCustomsDataStoreConnector.getEmail(any())(any())
      ) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          bind[IdentifierAction].to[FakeIdentifierAction],
          bind[CustomsDataStoreConnector].toInstance(mockCustomsDataStoreConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe NOT_FOUND
      }
    }
  }
}
