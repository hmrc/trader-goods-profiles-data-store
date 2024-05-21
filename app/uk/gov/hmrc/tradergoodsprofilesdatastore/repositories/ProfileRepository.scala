package uk.gov.hmrc.tradergoodsprofilesdatastore.repositories

import uk.gov.hmrc.tradergoodsprofilesdatastore.models.Profile
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProfileRepository @Inject() (
                                    mongoComponent: MongoComponent,
                                  )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[Profile](
    collectionName = "profiles",
    mongoComponent = mongoComponent,
    domainFormat = Profile.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("eori"),
        IndexOptions().name("eori")
      )
    )
  ) {

  private def byEori(eori: String): Bson = Filters.equal("eori", eori)

  def get(eori: String): Future[Option[Profile]] =
      collection
        .find(byEori(eori))
        .headOption()

  def set(profile: Profile): Future[Boolean] =
    collection
      .replaceOne(
        filter = byEori(profile.eori),
        replacement = profile,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)

}
