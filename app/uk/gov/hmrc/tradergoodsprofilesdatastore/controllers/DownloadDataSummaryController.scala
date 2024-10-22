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
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.DownloadDataNotification
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.{SdesService, UuidService}

import java.time.temporal.ChronoUnit
import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryController @Inject() (
  downloadDataSummaryRepository: DownloadDataSummaryRepository,
  routerConnector: RouterConnector,
  secureDataExchangeProxyConnector: SecureDataExchangeProxyConnector,
  cc: ControllerComponents,
  identify: IdentifierAction,
  uuidService: UuidService,
  clock: Clock,
  sdesService: SdesService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getDownloadDataSummaries(eori: String): Action[AnyContent] = identify.async {
    downloadDataSummaryRepository
      .get(eori)
      .map(summaries => Ok(Json.toJson(summaries)))
  }

  private def buildRetentionDays(notification: DownloadDataNotification): Future[String] =
    notification.metadata.find(x => x.metadata == "RETENTION_DAYS") match {
      case Some(metadata) => Future.successful(metadata.value)
      case _              =>
        Future.failed(new RuntimeException(s"Retention days not found in notification for EORI: ${notification.eori}"))
    }

  private def handleGetOldestInProgress(eori: String): Future[DownloadDataSummary] =
    downloadDataSummaryRepository.getOldestInProgress(eori).flatMap {
      case Some(downloadDataSummary) => Future.successful(downloadDataSummary)
      case None                      =>
        Future.failed(
          new RuntimeException(s"Initial download request (download data summary) not found for EORI: $eori")
        )
    }

  def submitNotification(): Action[DownloadDataNotification] =
    Action.async(parse.json[DownloadDataNotification]) { implicit request =>
      val notification = request.body
      for {
        retentionDays       <- buildRetentionDays(notification)
        //TODO match on conversation ID
        downloadDataSummary <- handleGetOldestInProgress(notification.eori)
        newSummary           = DownloadDataSummary(
                                 downloadDataSummary.summaryId,
                                 notification.eori,
                                 FileReadyUnseen,
                                 downloadDataSummary.createdAt,
                                 //TODO handle toInt fail?
                                 clock.instant.plus(retentionDays.toInt, ChronoUnit.DAYS),
                                 Some(FileInfo(notification.fileName, notification.fileSize, retentionDays))
                               )
        _                   <- downloadDataSummaryRepository.set(newSummary)
        submissionId         = uuidService.generate()
        _                   <- sdesService.enqueueSubmission(submissionId, newSummary.eori, retentionDays, newSummary.summaryId)
      } yield NoContent
    }

  def requestDownloadData(eori: String): Action[AnyContent] = identify.async { implicit request =>
    routerConnector.getRequestDownloadData(eori).flatMap { _ =>
      val createdAt = clock.instant
      downloadDataSummaryRepository
        .set(
          DownloadDataSummary(
            uuidService.generate(),
            eori,
            FileInProgress,
            createdAt,
            createdAt.plus(30, ChronoUnit.DAYS),
            None
          )
        )
        .map(_ => Accepted)
    }
  }

  def getDownloadData(eori: String): Action[AnyContent] = identify.async { implicit request =>
    secureDataExchangeProxyConnector.getFilesAvailableUrl(eori).map { downloadDatas =>
      Ok(Json.toJson(downloadDatas))
    }
  }
}
