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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeIdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{ProfileRequest, ProfileResponse}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.ProfileRepository

import scala.concurrent.{ExecutionContext, Future}

class ProfileControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val requestEori = "GB123456789099"

  val profileRequest = ProfileRequest(
    actorId = "GB123456789099",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345")
  )

  private val baseUrl                = "/trader-goods-profiles-data-store"
  private val setUrl                 = routes.ProfileController.setProfile(requestEori).url
  private val getUrl                 = routes.ProfileController.getProfile(requestEori).url
  private val doesExistUrl           = routes.ProfileController.doesProfileExist(requestEori).url
  private val validFakePostRequest   =
    FakeRequest("POST", setUrl, FakeHeaders(Seq(CONTENT_TYPE -> JSON)), Json.toJson(profileRequest))
  private val invalidFakePostRequest = FakeRequest("POST", setUrl, FakeHeaders(Seq(CONTENT_TYPE -> JSON)), "{}")
  private val validFakeGetRequest    = FakeRequest("GET", getUrl)
  private val validDoesExistRequest  = FakeRequest("HEAD", doesExistUrl)

  private val expectedProfileResponse = ProfileResponse(
    eori = "1234567890",
    actorId = "1234567890",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = Some("123456"),
    niphlNumber = Some("123456")
  )

  s"POST $setUrl" - {

    "return 200 when valid data is posted" in {

      val mockProfileRepository = mock[ProfileRepository]
      when(mockProfileRepository.set(any(), any())) thenReturn Future.successful(true)

      val mockRouterConnector = mock[RouterConnector]
      when(mockRouterConnector.submitTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository),
          inject.bind[RouterConnector].toInstance(mockRouterConnector)
        )
        .build()

      running(application) {
        val result = route(application, validFakePostRequest).value
        status(result) shouldBe Status.OK
      }
    }

    "return 400 when invalid data is posted" in {

      val mockProfileRepository = mock[ProfileRepository]
      when(mockProfileRepository.set(any(), any())) thenReturn Future.successful(true)

      val mockRouterConnector = mock[RouterConnector]
      when(mockRouterConnector.submitTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository),
          inject.bind[IdentifierAction].to[FakeIdentifierAction]
        )
        .build()

      running(application) {
        val result = route(application, invalidFakePostRequest).value
        status(result) shouldBe Status.BAD_REQUEST
      }
    }

  }

  s"GET $getUrl" - {

    "return 200 when data is found" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.successful(Some(expectedProfileResponse))

      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK

        contentAsString(result) mustBe Json.toJson(expectedProfileResponse).toString
      }
    }

    "return 404 when data is not found" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }
    }

  }

  s"HEAD $doesExistUrl" - {

    "return 200 when profile exist" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.successful(Some(expectedProfileResponse))

      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validDoesExistRequest).value
        status(result) shouldBe Status.OK
      }
    }

    "return 404 when profile does not exist" in {

      val mockProfileRepository = mock[ProfileRepository]

      when(mockProfileRepository.get(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validDoesExistRequest).value
        status(result) shouldBe Status.NOT_FOUND
      }
    }

  }
}
