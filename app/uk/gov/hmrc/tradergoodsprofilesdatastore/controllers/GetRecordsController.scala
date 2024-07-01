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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.CheckRecordsRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GetRecordsController @Inject() (
  checkRecordsRepository: CheckRecordsRepository,
  cc: ControllerComponents,
  routerConnector: RouterConnector,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getRecords(
    eori: String,
    page: Option[Int],
    size: Option[Int]
  ): Action[AnyContent] = identify.async { implicit request =>
    for {
      httpResponse   <- routerConnector.getRecords(eori, page = page, size = size)
      recordsResponse = httpResponse.json.as[GetRecordsResponse]
    } yield Ok(Json.toJson(recordsResponse))
  }

  def checkRecords(eori: String): Action[AnyContent] = identify.async { implicit request =>
    checkRecordsRepository
      .get(eori)
      .map {
        case Some(_) => Ok
        case None    => NotFound
      }
  }
}
