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
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{IdentifierAction, RetireFileAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadySeen, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.DownloadDataNotification
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryController @Inject() (
  downloadDataSummaryRepository: DownloadDataSummaryRepository,
  routerConnector: RouterConnector,
  secureDataExchangeProxyConnector: SecureDataExchangeProxyConnector,
  cc: ControllerComponents,
  identify: IdentifierAction,
  retireFile: RetireFileAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getDownloadDataSummary(eori: String): Action[AnyContent] = (identify andThen retireFile).async {
    implicit request =>
      downloadDataSummaryRepository
        .get(eori)
        .map {
          case Some(summary) =>
            Ok(Json.toJson(summary))
          case _             => NotFound
        }
  }

  def submitNotification(): Action[DownloadDataNotification] =
    Action.async(parse.json[DownloadDataNotification]) { implicit request =>
      val notification  = request.body
      val retentionDays = notification.metadata.find(x => x.metadata == "RETENTION_DAYS") match {
        case Some(metadata) => metadata.value
        case None           => "30"
      }
      val summary       = DownloadDataSummary(
        notification.eori,
        FileReadyUnseen,
        Some(FileInfo(notification.fileName, notification.fileSize, Instant.now, retentionDays))
      )
      downloadDataSummaryRepository.set(summary).map { _ =>
        //TODO send email
        NoContent
      }
    }

  def requestDownloadData(eori: String): Action[AnyContent] = identify.async { implicit request =>
    routerConnector.getRequestDownloadData(eori).flatMap { _ =>
      downloadDataSummaryRepository
        .set(DownloadDataSummary(eori, FileInProgress, None))
        .map(_ => Accepted)
    }
  }

  def getDownloadData(eori: String): Action[AnyContent] = (identify andThen retireFile).async { implicit request =>
    downloadDataSummaryRepository
      .get(eori)
      .flatMap {
        case Some(summary) if summary.status == FileReadyUnseen || summary.status == FileReadySeen =>
          summary.fileInfo match {
            case Some(info) =>
              secureDataExchangeProxyConnector.getFilesAvailableUrl(eori).flatMap { downloadDatas =>
                downloadDatas.find(downloadData =>
                  downloadData.filesize == info.fileSize && downloadData.filename == info.fileName && downloadData.metadata
                    .find(metadataObject => metadataObject.metadata == "RETENTION_DAYS")
                    .get
                    .value == info.retentionDays
                ) match {
                  case Some(downloadData) =>
                    summary.status match {
                      case FileReadyUnseen =>
                        downloadDataSummaryRepository
                          .set(DownloadDataSummary(request.eori, FileReadySeen, summary.fileInfo))
                          .map { _ =>
                            Ok(Json.toJson(downloadData))
                          }
                      case _               => Future.successful(Ok(Json.toJson(downloadData)))
                    }
                  case None               => Future.successful(NotFound)
                }
              }
            case _          => Future.successful(NotFound)
          }
        case _                                                                                     => Future.successful(NotFound)
      }
  }
}
