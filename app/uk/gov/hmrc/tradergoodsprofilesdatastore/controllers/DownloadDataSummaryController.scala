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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, EmailConnector, RouterConnector, SecureDataExchangeProxyConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.DownloadDataStatus.{FileInProgress, FileReadyUnseen}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.DownloadRecordEmailParameters
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.DownloadDataNotification
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.{DownloadDataSummary, FileInfo}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.DownloadDataSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.UuidService
import uk.gov.hmrc.tradergoodsprofilesdatastore.utils.DateTimeFormats.dateTimeFormat

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}
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
  config: DataStoreAppConfig,
  uuidService: UuidService,
  clock: Clock
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
      (for {
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
        //TODO think about if we should separate email and submitting, a successful submit should not be related to if the email sends - workqueue
        _                   <- sendEmailNotification(notification.eori, retentionDays)
      } yield NoContent).recover { case e: EmailNotFoundException =>
        logger.info(e.getMessage)

        //TODO do we need a not found?
        NotFound
      }
    }

  private def sendEmailNotification(eori: String, retentionDays: String)(implicit hc: HeaderCarrier): Future[Done] = {
    //TODO determine when to send email in english or welsh (default is english) TGP-2654
    val isWelsh = false
    //TODO add flag into qa config as false until the email stuff is working
    if (config.sendNotificationEmail) {
      customsDataStoreConnector.getEmail(eori).flatMap {
        case Some(email) =>
          emailConnector
            .sendDownloadRecordEmail(
              email.address,
              DownloadRecordEmailParameters(
                convertToDateString(clock.instant.plus(retentionDays.toInt, ChronoUnit.DAYS), isWelsh)
              )
            )
        case None        => Future.failed(EmailNotFoundException(eori))
      }
    } else {
      Future.successful(Done)
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
