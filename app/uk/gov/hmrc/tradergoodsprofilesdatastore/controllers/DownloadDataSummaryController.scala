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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.FileInProgress
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataSummary
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DownloadDataSummaryController @Inject() (
  downloadDataSummaryRepository: DownloadDataSummaryRepository,
  routerConnector: RouterConnector,
  cc: ControllerComponents,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getDownloadDataSummary(eori: String): Action[AnyContent] = identify.async { implicit request =>
    downloadDataSummaryRepository
      .get(eori)
      .map {
        _.map { summary =>
          Ok(Json.toJson(summary))
        }.getOrElse(NotFound)
      }
  }

  def requestDownloadData(eori: String): Action[AnyContent] = identify.async { implicit request =>
    routerConnector.getRequestDownloadData(eori).flatMap { _ =>
      downloadDataSummaryRepository
        .set(DownloadDataSummary(eori, FileInProgress))
        .map(_ => Accepted)
    }
  }

  def updateDownloadDataStatus(eori: String): Action[DownloadDataSummary] =
    identify.async(parse.json[DownloadDataSummary]) { implicit request =>
      downloadDataSummaryRepository
        .update(eori, request.body.status)
        .map {
          case true  => NoContent
          case false => NotFound
        }
    }
}
