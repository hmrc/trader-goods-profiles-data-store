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

import org.mongodb.scala.model.Filters
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.Profile
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with PlayMongoRepositorySupport[Profile]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareDatabase()
  }

  private val profile = Profile("test-eori", "test-actor-id", "test-ukims", Some("test-nirms"), Some("test-niphl"))

  protected override val repository = new ProfileRepository(mongoComponent = mongoComponent)

  private def byEori = {
    Filters.equal("_id", profile.eori)
  }

  ".set" - {

    "must create a record when there is none" in {
      val setResult     = repository.set(profile).futureValue
      val updatedRecord = find(byEori).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual profile
    }

    "must update a record when there is one" in {
      insert(profile).futureValue
      val expectedProfile = profile.copy(ukimsNumber = "new-ukims")
      val setResult = repository.set(expectedProfile).futureValue
      val updatedRecord = find(byEori).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual expectedProfile
    }
  }

  ".get" - {

    "when there is a record for this eori" - {
      "must get the record" in {
        insert(profile).futureValue
        val result         = repository.get(profile.eori).futureValue
        result.value mustEqual profile
      }
    }

    "when there is no record for this ieorid" - {
      "must return None" in {
        repository.get("eori that does not exist").futureValue must not be defined
      }
    }

  }

}
