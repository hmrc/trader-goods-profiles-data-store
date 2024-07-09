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

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.CheckRecords
import scala.concurrent.ExecutionContext.Implicits.global

class CheckRecordsRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with PlayMongoRepositorySupport[CheckRecords]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar
    with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    prepareDatabase()
  }

  val sampleCheckRecords: CheckRecords = CheckRecords(
    eori = "GB123456789001"
  )

  protected override val repository = new CheckRecordsRepository(mongoComponent = mongoComponent)

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  ".set" - {

    "must create a checkRecords when there is none" in {
      val setResult     = repository.set(sampleCheckRecords.eori).futureValue
      val updatedRecord = find(byEori(sampleCheckRecords.eori)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual sampleCheckRecords
    }

    "must update a record when there is one" in {
      insert(sampleCheckRecords).futureValue

      val setResult     = repository.set(sampleCheckRecords.eori).futureValue
      val updatedRecord = find(byEori(sampleCheckRecords.eori)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual sampleCheckRecords
    }
  }

  ".get" - {

    "when there is a record for this eori it must get the record" in {
      insert(sampleCheckRecords).futureValue
      val result = repository.get(sampleCheckRecords.eori).futureValue
      result.value mustEqual sampleCheckRecords
    }

    "when there is no record for this eori it must return None" in {
      repository.get(sampleCheckRecords.eori).futureValue must not be defined
    }

  }

}
