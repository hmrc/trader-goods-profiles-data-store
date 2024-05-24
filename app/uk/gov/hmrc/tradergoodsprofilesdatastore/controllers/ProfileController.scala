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
import play.api.libs.json.{JsObject, JsString, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.ProfileRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.ProfileRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ProfileController @Inject() (
  profileRepository: ProfileRepository,
  cc: ControllerComponents,
  routerConnector: RouterConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def setProfile(eori: String): Action[ProfileRequest] = Action.async(parse.json[ProfileRequest]) { implicit request =>
    routerConnector.submitTraderProfile(request.body, eori).flatMap { case Done =>
      profileRepository.set(eori, request.body).map(_ => Ok)
    }
  }

  def getProfile(eori: String): Action[AnyContent] = Action.async {
    profileRepository
      .get(eori)
      .map {
        case Some(profile) => Ok(Json.toJson(profile))
        case None          => NotFound
      }
  }

  def doesProfileExist(eori: String): Action[AnyContent] = Action.async {
    profileRepository
      .get(eori)
      .map {
        case Some(profile) => Ok
        case None          => NotFound
      }
  }

}
