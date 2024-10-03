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

package uk.gov.hmrc.tradergoodsprofilesdatastore.repositories

import org.apache.pekko.Done
import org.mongodb.scala.model.Filters
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.ProfileRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.ProfileResponse

import scala.concurrent.ExecutionContext.Implicits.global

class ProfileRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with PlayMongoRepositorySupport[ProfileResponse]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareDatabase()
  }

  private val profileEori     = "test-eori"
  private val profileRequest  = ProfileRequest("test-actor-id", "test-ukims", Some("test-nirms"), Some("test-niphl"))
  private val profileResponse =
    ProfileResponse(profileEori, "test-actor-id", "test-ukims", Some("test-nirms"), Some("test-niphl"))

  protected override val repository = new ProfileRepository(mongoComponent = mongoComponent)

  private def byEori =
    Filters.equal("eori", profileResponse.eori)

  ".set" - {

    "must create a record when there is none" in {
      val setResult     = repository.set(profileEori, profileRequest).futureValue
      val updatedRecord = find(byEori).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual profileResponse
    }

    "must update a record when there is one" in {
      insert(profileResponse).futureValue
      val modifiedProfileRequest  = profileRequest.copy(ukimsNumber = "new-ukims")
      val expectedProfileResponse = profileResponse.copy(ukimsNumber = "new-ukims")

      val setResult     = repository.set(profileEori, modifiedProfileRequest).futureValue
      val updatedRecord = find(byEori).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual expectedProfileResponse
    }
  }

  ".get" - {

    "when there is a record for this eori it must get the record" in {
      insert(profileResponse).futureValue
      val result = repository.get(profileEori).futureValue
      result.value mustEqual profileResponse
    }

    "when there is no record for this eori it must return None" in {
      repository.get("eori that does not exist").futureValue must not be defined
    }

  }

  ".deleteAll" - {
    "it mush drop the profiles collection" in {
      insert(profileResponse).futureValue
      val result      = repository.deleteAll().futureValue
      val recordCheck = repository.get(profileEori).futureValue
      result mustEqual Done
      recordCheck mustBe None
    }
  }

  ".updateEori" - {

    "must update profile when there is a one" in {
      val newEori      = "new-eori"
      insert(profileResponse).futureValue
      val updateResult = repository.updateEori(profileResponse.eori, newEori).futureValue

      val result = repository.get(newEori).futureValue

      updateResult mustEqual true
      result.value.eori mustEqual newEori
    }

    "must not update profile when there is none" in {
      val newEori      = "new-eori"
      val updateResult = repository.updateEori(profileResponse.eori, newEori).futureValue

      val result = repository.get(newEori).futureValue

      updateResult mustEqual false
      result mustBe None
    }
  }
}
