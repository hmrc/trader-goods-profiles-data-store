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
import play.api.inject
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeIdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.EmailNotificationRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.EmailNotificationsRepository

import scala.concurrent.{ExecutionContext, Future}

class EmailNotificationControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global
  private val testEori              = "GB123456789099"
  private val testRecordId          = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"
  private val testExpiredDate       = "05 September 2024"

  private val notificationRequest = EmailNotificationRequest(testExpiredDate)

  private lazy val createUrl = routes.EmailNotificationController.create(testEori, testRecordId).url

  s"POST $createUrl" - {

    "return 200 when record is successfully created" in {

      val mockNotificationsRepository = mock[EmailNotificationsRepository]
      when(mockNotificationsRepository.create(any(), any(), any())) thenReturn Future.successful(Done)

      val application = applicationBuilder()
        .overrides(
          inject.bind[IdentifierAction].to[FakeIdentifierAction],
          inject.bind[EmailNotificationsRepository].toInstance(mockNotificationsRepository)
        )
        .build()
      running(application) {
        val request = FakeRequest("POST", createUrl)
          .withHeaders("Content-Type" -> "application/json")
          .withJsonBody(Json.toJson(notificationRequest))
        val result  = route(application, request).value
        status(result) shouldBe OK
      }
    }
  }
}
