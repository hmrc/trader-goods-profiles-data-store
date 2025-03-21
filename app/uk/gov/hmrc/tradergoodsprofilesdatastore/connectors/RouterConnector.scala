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

package uk.gov.hmrc.tradergoodsprofilesdatastore.connectors

import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.*
import play.api.libs.json._
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import sttp.model.Uri
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.Service
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.*
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{CorrelationId, GetRecordsResponse, GoodsItemRecord}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RouterConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private val baseUrlRouter: Service         = config.get[Service]("microservice.services.trader-goods-profiles-router")
  private val clientIdAndAcceptHeaders       =
    Seq("X-Client-ID" -> "tgp-frontend", "Accept" -> "application/vnd.hmrc.1.0+json")
  private def traderProfileUrl(eori: String) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori"

  private def tgpRecordsUri(
    eori: String,
    lastUpdatedDate: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Uri =
    uri"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"

  private def tgpDeleteRecordUrl(
    eori: String,
    recordId: String
  ) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$eori"

  private def tgpGetOrUpdateRecordUrl(
    eori: String,
    recordId: String
  ) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records/$recordId"

  private def createGoodsRecordUrl(eori: String) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records"

  private def adviceUrl(eori: String, recordId: String) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records/$recordId/advice"

  private def getRequestDownloadDataUrl(eori: String) =
    url"$baseUrlRouter/trader-goods-profiles-router/customs/traders/goods-profiles/$eori/download"

  def hasHistoricProfile(eori: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = url"$baseUrlRouter/trader-goods-profiles-router/customs/traders/goods-profiles/$eori"

    httpClient
      .get(url)
      .setHeader(clientIdAndAcceptHeaders *)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(true)
          case FORBIDDEN => Future.successful(false)
          case _         => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  }

  def createTraderProfile(traderProfile: ProfileRequest, eori: String)(implicit hc: HeaderCarrier): Future[Done] = {
    val url = url"$baseUrlRouter/trader-goods-profiles-router/customs/traders/goods-profiles/$eori"

    httpClient
      .post(url)
      .setHeader(clientIdAndAcceptHeaders *)
      .withBody(Json.toJson(traderProfile))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Done)
          case _  => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
  }

  def updateTraderProfile(traderProfile: ProfileRequest, eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .put(traderProfileUrl(eori))
      .setHeader(clientIdAndAcceptHeaders *)
      .withBody(Json.toJson(traderProfile))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Done)
          case _  => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def createRecord(
    createRecordRequest: CreateRecordRequest,
    eori: String
  )(implicit hc: HeaderCarrier): Future[GoodsItemRecord] =
    httpClient
      .post(createGoodsRecordUrl(eori))
      .setHeader(clientIdAndAcceptHeaders *)
      .withBody(Json.toJson(createRecordRequest))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case CREATED => Future.successful(response.json.as[GoodsItemRecord])
          case _       => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def getRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit hc: HeaderCarrier): Future[GetRecordsResponse] = {
    val uri = tgpRecordsUri(eori, lastUpdatedDate, page, size).toString()
    httpClient
      .get(url"$uri")
      .setHeader(clientIdAndAcceptHeaders *)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response.json.as[GetRecordsResponse])
          case _  => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
  }

  def getRecord(
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): Future[Option[GoodsItemRecord]] =
    httpClient
      .get(tgpGetOrUpdateRecordUrl(eori, recordId))
      .setHeader(clientIdAndAcceptHeaders *)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK                      => Future.successful(Some(response.json.as[GoodsItemRecord]))
          case NOT_FOUND | BAD_REQUEST => Future.successful(None)
          case _                       => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def deleteRecord(
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .delete(tgpDeleteRecordUrl(eori, recordId))
      .setHeader(clientIdAndAcceptHeaders *)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT              => Future.successful(true)
          case NOT_FOUND | BAD_REQUEST => Future.successful(false)
          case _                       => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def patchRecord(
    updateRecord: PatchRecordRequest,
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .patch(tgpGetOrUpdateRecordUrl(eori, recordId))
      .setHeader(clientIdAndAcceptHeaders *)
      .withBody(Json.toJson(updateRecord))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK          => Future.successful(true)
          case NOT_FOUND   => Future.successful(false)
          case BAD_REQUEST => handleBadRequest(response)
          case _           => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  private def handleBadRequest(response: HttpResponse): Future[Boolean] = {
    val json = Json.parse(response.body)
    (json \ "errors").asOpt[List[JsObject]] match {
      case Some(errors) =>
        val errorNumbers             = errors.flatMap(err => (err \ "errorNumber").asOpt[Int])
        val DuplicateProductRefError = 10
        val RecordRemovedError       = 31
        if (errorNumbers.exists(num => num == DuplicateProductRefError || num == RecordRemovedError)) {
          Future.successful(false)
        } else {
          Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      case None         =>
        Future.failed(UpstreamErrorResponse(response.body, response.status))
    }
  }

  def putRecord(
    updateRecord: PutRecordRequest,
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .put(tgpGetOrUpdateRecordUrl(eori, recordId))
      .setHeader(clientIdAndAcceptHeaders *)
      .withBody(Json.toJson(updateRecord))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(true)
          case NOT_FOUND => Future.successful(false)
          case _         => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def requestAdvice(eori: String, recordId: String, advice: AdviceRequest)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .post(adviceUrl(eori, recordId))
      .setHeader(clientIdAndAcceptHeaders *)
      .withBody(Json.toJson(advice))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case CREATED => Future.successful(Done)
          case _       => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def withdrawAdvice(eori: String, recordId: String, withdrawReason: WithdrawReasonRequest)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .put(adviceUrl(eori, recordId))
      .setHeader(clientIdAndAcceptHeaders *)
      .withBody(Json.toJson(withdrawReason))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case NO_CONTENT => Future.successful(Done)
          case _          => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }

  def getRequestDownloadData(
    eori: String
  )(implicit hc: HeaderCarrier): Future[CorrelationId] =
    httpClient
      .get(getRequestDownloadDataUrl(eori))
      .setHeader(clientIdAndAcceptHeaders *)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case ACCEPTED => Future.successful(response.json.as[CorrelationId])
          case _        => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
}
