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

import play.api.Logging
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.UpdateRecordRequest

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class UpdateRecordController @Inject() (
  cc: ControllerComponents,
  routerConnector: RouterConnector,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def updateRecord(eori: String, recordId: String): Action[UpdateRecordRequest] =
    identify.async(parse.json[UpdateRecordRequest]) { implicit request =>
      routerConnector.updateRecord(request.body, eori, recordId) transform {
        case Success(_)                                                             => Success(Ok)
        case Failure(cause: UpstreamErrorResponse) if cause.statusCode == NOT_FOUND => Success(NotFound)
        case Failure(cause: UpstreamErrorResponse)                                  =>
          logger.error(s"Update record failed with ${cause.statusCode} with message: ${cause.message}")
          Success(InternalServerError)
      }
    }

}
