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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector, RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{IdentifierAction, RetireFilesAction}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.DownloadRecordEmailParameters
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{DownloadDataNotification, Email}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.DateTimeFormats.dateTimeFormat

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadDataSummaryController @Inject() (
  downloadDataSummaryRepository: DownloadDataSummaryRepository,
  routerConnector: RouterConnector,
  secureDataExchangeProxyConnector: SecureDataExchangeProxyConnector,
  customsDataStoreConnector: CustomsDataStoreConnector,
  emailConnector: EmailConnector,
  cc: ControllerComponents,
  identify: IdentifierAction,
  retireFiles: RetireFilesAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  //TODO TTL instead dof retire files
  def getDownloadDataSummaries(eori: String): Action[AnyContent] = (identify andThen retireFiles).async {
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

  private def handleGetEmail(eori: String)(implicit hc: HeaderCarrier): Future[Email] =
    customsDataStoreConnector.getEmail(eori).flatMap {
      case Some(email) => Future.successful(email)
      case None        => Future.failed(EmailNotFoundException(eori))
    }

  private def handleGetLatestInProgress(eori: String)(implicit hc: HeaderCarrier): Future[DownloadDataSummary] =
    downloadDataSummaryRepository.getLatestInProgress(eori).flatMap {
      case Some(downloadDataSummary) => Future.successful(downloadDataSummary)
      case None                      =>
        Future.failed(
          new RuntimeException(s"Initial download request (download data summary) not found for EORI: $eori")
        )
    }

  def submitNotification(): Action[DownloadDataNotification] =
    Action.async(parse.json[DownloadDataNotification]) { implicit request =>
      val notification = request.body
      //TODO determine when to send email in english or welsh (default is english) TGP-2654
      val isWelsh      = false
      (for {
        retentionDays       <- buildRetentionDays(notification)
        //TODO match on conversation ID
        downloadDataSummary <- handleGetLatestInProgress(notification.eori)
        newSummary           = DownloadDataSummary(
                                 downloadDataSummary.summaryId,
                                 notification.eori,
                                 FileReadyUnseen,
                                 downloadDataSummary.createdAt,
                                 //TODO use clock instead of instant - better for testing
                                 Some(FileInfo(notification.fileName, notification.fileSize, Instant.now, retentionDays))
                               )
        _                   <- downloadDataSummaryRepository.set(newSummary)
        //TODO think about if we should separate email and submitting, a successful submit should not be related to if the email sends - workqueue
        email               <- handleGetEmail(notification.eori)
        _                   <- emailConnector
                                 .sendDownloadRecordEmail(
                                   email.address,
                                   DownloadRecordEmailParameters(
                                     convertToDateString(Instant.now.plus(retentionDays.toInt, ChronoUnit.DAYS), isWelsh)
                                   )
                                 )
      } yield NoContent).recover { case e: EmailNotFoundException =>
        logger.info(e.getMessage)

        //TODO do we need a not found?
        NotFound
      }
    }

  private case class EmailNotFoundException(eori: String) extends RuntimeException {
    override def getMessage: String = s"Unable to find the email for EORI: $eori"
  }

  private def convertToDateString(instant: Instant, isWelsh: Boolean): String =
    instant
      .atZone(ZoneOffset.UTC)
      .toLocalDate
      .format(dateTimeFormat(if (isWelsh) { "cy" }
      else { "en" }))

  def requestDownloadData(eori: String): Action[AnyContent] = identify.async { implicit request =>
    routerConnector.getRequestDownloadData(eori).flatMap { _ =>
      downloadDataSummaryRepository
        .set(DownloadDataSummary(java.util.UUID.randomUUID().toString, eori, FileInProgress, Instant.now, None))
        .map(_ => Accepted)
    }
  }

  def getDownloadData(eori: String): Action[AnyContent] = (identify andThen retireFiles).async { implicit request =>
    secureDataExchangeProxyConnector.getFilesAvailableUrl(eori).map { downloadDatas =>
      Ok(Json.toJson(downloadDatas))
    }
  }
}
