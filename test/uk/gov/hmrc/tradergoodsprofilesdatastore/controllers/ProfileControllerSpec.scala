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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status
import play.api.inject
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.Profile
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.ProfileRepository

import scala.concurrent.{ExecutionContext, Future}

class ProfileControllerSpec extends AnyWordSpec with Matchers {

  implicit val ec = ExecutionContext.global

  val requestBody = JsObject(
    Map(
      "eori"        -> JsString("GB123456789099"),
      "actorId"     -> JsString("GB123456789099"),
      "ukimsNumber" -> JsString("XIUKIM47699357400020231115081800"),
      "nirmsNumber" -> JsString("RMS-GB-123456"),
      "niphlNumber" -> JsString("6 S12345")
    )
  )

  private val baseUrl                = "/trader-goods-profiles-data-store"
  private val setUrl                 = baseUrl + routes.ProfileController.setProfile.url
  private val getUrl                 = baseUrl + routes.ProfileController.getProfile("1234567890").url
  private val doesExistUrl           = baseUrl + routes.ProfileController.doesProfileExist("1234567890").url
  private val validFakePostRequest   = FakeRequest("POST", setUrl, FakeHeaders(Seq(CONTENT_TYPE -> JSON)), requestBody)
  private val invalidFakePostRequest = FakeRequest("POST", setUrl, FakeHeaders(Seq(CONTENT_TYPE -> JSON)), "{}")
  private val validFakeGetRequest    = FakeRequest("GET", getUrl)
  private val validDoesExistRequest  = FakeRequest("GET", doesExistUrl)

  private val profile = Profile(
    eori = "1234567890",
    actorId = "1234567890",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = Option("123456"),
    niphlNumber = Option("123456")
  )

  s"POST $setUrl" should {

    "return 200 when valid data is posted" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.set(any())) thenReturn Future.successful(true)

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.OK
      }
    }

    "return 400 when invalid data is posted" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.set(any())) thenReturn Future.successful(true)

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, invalidFakePostRequest).value
        status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "return 500 when repository failed" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.set(any())) thenReturn Future.failed(exception = new Exception("Session is failed"))

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  s"GET $getUrl" should {

    "return 200 when data is found" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.successful(Some(profile))

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK

        contentAsString(result) mustBe Json.toJson(profile).toString
      }
    }

    "return 404 when data is not found" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.successful(None)

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "return 500 when repository failed" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.failed(exception = new Exception("Session is failed"))

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  s"GET $doesExistUrl" should {

    "return 204 when profile exist" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.successful(Some(profile))

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validDoesExistRequest).value
        status(result) shouldBe Status.NO_CONTENT
      }
    }

    "return 404 when profile does not exist" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.successful(None)

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validDoesExistRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "return 500 when repository failed" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.failed(exception = new Exception("Session is failed"))

      val application = baseApplicationBuilder
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validDoesExistRequest).value
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
