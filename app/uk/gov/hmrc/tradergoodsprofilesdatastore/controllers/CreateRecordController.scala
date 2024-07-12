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
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.CreateRecordRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.CreateRecordResponse
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CreateRecordController @Inject() (
  recordsRepository: RecordsRepository,
  cc: ControllerComponents,
  routerConnector: RouterConnector,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def create(eori: String): Action[CreateRecordRequest] =
    identify.async(parse.json[CreateRecordRequest]) { implicit request =>
      routerConnector.createRecord(request.body).flatMap { goodsItemRecord =>
        recordsRepository
          .insert(goodsItemRecord)
          .map(_ => Ok(Json.toJson(CreateRecordResponse(goodsItemRecord.recordId))))
      }
    }

}