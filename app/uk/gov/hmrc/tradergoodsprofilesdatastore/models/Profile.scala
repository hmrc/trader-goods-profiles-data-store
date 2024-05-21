package uk.gov.hmrc.tradergoodsprofilesdatastore.models

import play.api.libs.json.{Json, OFormat}

case class Profile (
                     ukims: String,
                     nirms: Option[String],
                     niphl: Option[String]
                   )

object Profile {
  implicit val format: OFormat[Profile] = Json.format[Profile]
}