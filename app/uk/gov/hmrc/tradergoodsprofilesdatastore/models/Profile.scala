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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models

import play.api.libs.json.{OFormat, OWrites, Reads, __}


case class Profile (
                     eori: String,
                     actorId: String,
                     ukimsNumber: String,
                     nirmsNumber: Option[String],
                     niphlNumber: Option[String]
                   )

object Profile {

  val reads: Reads[Profile] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "eori").read[String] and
        (__ \ "actorId").read[String] and
        (__ \ "ukimsNumber").read[String] and
        (__ \ "nirmsNumber").readNullable[String] and
        (__ \ "niphlNumber").readNullable[String]
      )(Profile.apply _)
  }

  val writes: OWrites[Profile] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "eori").write[String] and
        (__ \ "actorId").write[String] and
        (__ \ "ukimsNumber").write[String] and
        (__ \ "nirmsNumber").writeNullable[String] and
        (__ \ "niphlNumber").writeNullable[String]
      )(unlift(Profile.unapply))
  }

  implicit val format: OFormat[Profile] = OFormat(reads, writes)
}
