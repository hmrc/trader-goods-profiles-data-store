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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Headers}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.DownloadDataNotification
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.SdesService

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
  clock: Clock,
  config: DataStoreAppConfig,
  sdesService: SdesService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getDownloadDataSummaries(eori: String): Action[AnyContent] = identify.async {
    downloadDataSummaryRepository
      .get(eori)
      .map(summaries => Ok(Json.toJson(summaries)))
  }

  def touchDownloadDataSummaries(eori: String): Action[AnyContent] = identify.async {
    downloadDataSummaryRepository
      .updateSeen(eori)
      .map(_ => NoContent)
  }

  private def buildRetentionDays(notification: DownloadDataNotification): Future[String] =
    notification.metadata.find(x => x.metadata == "RETENTION_DAYS") match {
      case Some(metadata) => Future.successful(metadata.value)
      case _              =>
        Future.failed(new RuntimeException(s"Retention days not found in notification for EORI: ${notification.eori}"))
    }

  private def handleConversationId(eori: String, headers: Headers): Future[String] =
    headers.get("x-conversation-id") match {
      case Some(conversationId) => Future.successful(conversationId)
      case _                    =>
        Future.failed(new RuntimeException(s"Header x-conversation-id not present in notification for EORI: $eori"))
    }

  private def handleGetDownloadDataSummary(eori: String, summaryId: String): Future[DownloadDataSummary] =
    downloadDataSummaryRepository.get(eori, summaryId).flatMap {
      case Some(downloadDataSummary) => Future.successful(downloadDataSummary)
      case None                      =>
        Future.failed(
          new RuntimeException(s"Initial download request not found for EORI: $eori")
        )
    }

  private def handleToInt(string: String): Future[Int] =
    try Future.successful(string.toInt)
    catch {
      case _: Throwable =>
        Future.failed(
          new RuntimeException(s"$string is not a number so cannot be used for retention days")
        )
    }

  def submitNotification(): Action[DownloadDataNotification] =
    Action.async(parse.json[DownloadDataNotification]) { implicit request =>
      val notification = request.body
      for {
        conversationId      <- handleConversationId(notification.eori, request.headers)
        retentionDays       <- buildRetentionDays(notification)
        retentionDaysAsInt  <- handleToInt(retentionDays)
        downloadDataSummary <- handleGetDownloadDataSummary(notification.eori, conversationId)
        newSummary           = DownloadDataSummary(
                                 downloadDataSummary.summaryId,
                                 notification.eori,
                                 FileReadyUnseen,
                                 downloadDataSummary.createdAt,
                                 clock.instant.plus(retentionDaysAsInt, ChronoUnit.DAYS),
                                 Some(FileInfo(notification.fileName, notification.fileSize, retentionDays))
                               )
        _                   <- downloadDataSummaryRepository.set(newSummary)
        _                   <- handleEnqueueSubmission(newSummary)
      } yield NoContent
    }

  private def handleEnqueueSubmission(downloadDataSummary: DownloadDataSummary): Future[Done] =
    if (config.sendNotificationEmail) {
      sdesService.enqueueSubmission(downloadDataSummary)
    } else {
      Future.successful(Done)
    }

  def requestDownloadData(eori: String): Action[AnyContent] = identify.async { implicit request =>
    routerConnector.getRequestDownloadData(eori).flatMap { correlationId =>
      val createdAt = clock.instant
      downloadDataSummaryRepository
        .set(
          DownloadDataSummary(
            correlationId.correlationId,
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
