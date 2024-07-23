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
import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{IdentifierAction, StoreLatestAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DeleteRecordController @Inject() (
  cc: ControllerComponents,
  routerConnector: RouterConnector,
  recordsRepository: RecordsRepository,
  identify: IdentifierAction,
  storeLatestAction: StoreLatestAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def deleteRecord(eori: String, recordId: String): Action[AnyContent] = (identify andThen storeLatestAction).async {
    implicit request =>
      isRecordActive(eori, recordId).flatMap {
        case true  =>
          makeRecordInactive(eori, recordId) transform {
            case Success(_)                            => Success(NoContent)
            case Failure(cause: UpstreamErrorResponse) =>
              logger.error(s"Deleted record failed with ${cause.statusCode} with message: ${cause.message}")
              Success(InternalServerError)
          }
        case false =>
          makeRecordInactive(eori, recordId) transform {
            case Success(_)                                                               => Success(NotFound)
            case Failure(cause: UpstreamErrorResponse) if cause.statusCode == BAD_REQUEST => Success(NotFound)
            case Failure(cause: UpstreamErrorResponse)                                    =>
              logger.error(s"Deleted record failed with ${cause.statusCode} with message: ${cause.message}")
              Success(InternalServerError)
          }
      }
  }

  private def makeRecordInactive(eori: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    routerConnector.deleteRecord(eori, recordId).map(response => response)

  private def isRecordActive(eori: String, recordId: String): Future[Boolean] =
    recordsRepository.get(eori, recordId).map {
      case Some(record) => record.active
      case None         => false
    }
}
