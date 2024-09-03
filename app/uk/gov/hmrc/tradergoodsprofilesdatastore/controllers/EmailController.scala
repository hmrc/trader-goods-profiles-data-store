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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, RouterConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{IdentifierAction, StoreLatestAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.GetRecordsResponse
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.Pagination.{buildPagination, startingPage}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class EmailController @Inject() (
  customsDataStoreConnector: CustomsDataStoreConnector,
  cc: ControllerComponents,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getEmail(eori: String): Action[AnyContent] = identify.async { implicit request =>
    customsDataStoreConnector
      .getEmail(eori)
      .map {
        case Some(email) => Ok(Json.toJson(email))
        case None        => NotFound
      }
  }
}