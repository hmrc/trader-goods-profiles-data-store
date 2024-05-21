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

  private val profile = Profile("test-eori", "test-ukims", Some("test-nirms"), Some("test-niphl"))

  protected override val repository = new ProfileRepository(mongoComponent = mongoComponent)

  ".set" - {

    "must create a record when there is none" in {
      val setResult     = repository.set(profile).futureValue
      val updatedRecord = find(Filters.equal("eori", profile.eori)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual profile
    }

    "must update a record when there is one" in {
      insert(profile).futureValue
      val expectedProfile = profile.copy(ukims = "new-ukims")
      val setResult = repository.set(expectedProfile).futureValue
      val updatedRecord = find(Filters.equal("eori", profile.eori)).futureValue.headOption.value

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
