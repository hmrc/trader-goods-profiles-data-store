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
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsSummaryRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RecordsSummaryController @Inject() (
  recordsSummaryRepository: RecordsSummaryRepository,
  cc: ControllerComponents,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def recordsSummary: Action[AnyContent] = identify.async { implicit request =>
    recordsSummaryRepository
      .get(request.eori)
      .map {
        _.map { summary =>
          Ok(Json.toJson(summary))
        }.getOrElse(NotFound)
      }
  }
}
