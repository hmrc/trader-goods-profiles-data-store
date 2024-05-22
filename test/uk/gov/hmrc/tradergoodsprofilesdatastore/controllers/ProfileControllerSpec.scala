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
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status
import play.api.inject
import play.api.libs.json.{JsBoolean, JsObject, JsString}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers, ResultExtractors}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.ProfileRepository

import scala.concurrent.{ExecutionContext, Future}

class ProfileControllerSpec extends AnyWordSpec with Matchers {

  implicit val ec = ExecutionContext.global

  val requestBody = JsObject(
    Map(
      "eori" -> JsString("GB123456789099"),
      "actorId" -> JsString("GB123456789099"),
      "ukimsNumber" -> JsString("XIUKIM47699357400020231115081800"),
      "nirmsNumber" -> JsString("RMS-GB-123456"),
      "niphlNumber" -> JsString("6 S12345")
    )
  )

  private val setUrl = "/trader-goods-profiles-data-store/tgp/set-profile"
  private val fakePostRequest = FakeRequest("POST", setUrl, FakeHeaders(Seq()), requestBody)
  private val invalidFakePostRequest = FakeRequest("POST", setUrl, FakeHeaders(Seq()), "{}")

  "POST /" should {

    "return 200 when valid data is posted" in {


      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.set(any())) thenReturn Future.successful(true)

      val application = baseApplicationBuilder.overrides(
        inject.bind[ProfileRepository].toInstance(mockProfileRepository)
      ).build()

      running(application) {
        val result = route(application, fakePostRequest).value
        status(result) shouldBe Status.OK
      }
    }

    "return 400 when invalid data is posted" in {


      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.set(any())) thenReturn Future.successful(true)

      val application = baseApplicationBuilder.overrides(
        inject.bind[ProfileRepository].toInstance(mockProfileRepository)
      ).build()

      running(application) {
        val result = route(application, invalidFakePostRequest).value
        status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 500 when repository failed" in {


      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.set(any())) thenReturn Future.failed(exception = new Exception("Session is failed"))

      val application = baseApplicationBuilder.overrides(
        inject.bind[ProfileRepository].toInstance(mockProfileRepository)
      ).build()

      running(application) {
        val result = route(application, fakePostRequest).value
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
