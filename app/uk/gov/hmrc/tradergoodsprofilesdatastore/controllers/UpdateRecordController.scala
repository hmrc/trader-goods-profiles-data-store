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

import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.{PatchRecordRequest, PutRecordRequest}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UpdateRecordController @Inject() (
  cc: ControllerComponents,
  routerConnector: RouterConnector,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def patchRecord(recordId: String): Action[PatchRecordRequest] =
    identify.async(parse.json[PatchRecordRequest]) { implicit request =>
      routerConnector.patchRecord(request.body, request.eori, recordId).map {
        case true  => Ok
        case false => NotFound
      }
    }

  def putRecord(recordId: String): Action[PutRecordRequest] =
    identify.async(parse.json[PutRecordRequest]) { implicit request =>
      routerConnector.putRecord(request.body, request.eori, recordId).map {
        case true  => Ok
        case false => NotFound
      }
    }
}
