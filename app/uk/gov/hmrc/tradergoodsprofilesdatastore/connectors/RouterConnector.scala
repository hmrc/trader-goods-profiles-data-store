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

import uk.gov.hmrc.tradergoodsprofilesdatastore.config.Service
import org.apache.pekko.Done
import play.api.Configuration
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.{CreateRecordRequest, ProfileRequest, UpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{GetRecordsResponse, GoodsItemRecord}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RouterConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private val baseUrlRouter: Service         = config.get[Service]("microservice.services.trader-goods-profiles-router")
  private val clientIdHeader                 = ("X-Client-ID", "tgp-frontend")
  private def traderProfileUrl(eori: String) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori"

  private def tgpRecordsUri(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  ) =
    uri"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"

  private def tgpDeleteRecordUrl(
    eori: String,
    recordId: String,
    actorId: String
  ) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records/$recordId?actorId=$actorId"

  private def tgpUpdateRecordUrl(
    eori: String,
    recordId: String
  ) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records/$recordId"

  private def createGoodsRecordUrl(eori: String) =
    url"$baseUrlRouter/trader-goods-profiles-router/traders/$eori/records"

  def submitTraderProfile(traderProfile: ProfileRequest, eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .put(traderProfileUrl(eori))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(traderProfile))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Done
        }
      }

  def createRecord(
    createRecordRequest: CreateRecordRequest
  )(implicit hc: HeaderCarrier): Future[GoodsItemRecord] =
    httpClient
      .post(createGoodsRecordUrl(createRecordRequest.eori))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(createRecordRequest))
      .execute[HttpResponse]
      .map(response => response.json.as[GoodsItemRecord])

  def getRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit hc: HeaderCarrier): Future[GetRecordsResponse] = {
    val uri = tgpRecordsUri(eori, lastUpdatedDate, page, size).toString()
    httpClient
      .get(url"$uri")
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map(response => response.json.as[GetRecordsResponse])
  }

  def deleteRecord(
    eori: String,
    recordId: String,
    actorId: String
  )(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .delete(tgpDeleteRecordUrl(eori, recordId, actorId))
      .setHeader(clientIdHeader)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case NO_CONTENT => Done
        }
      }

  def updateRecord(
    updateRecord: UpdateRecordRequest,
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .patch(tgpUpdateRecordUrl(eori, recordId))
      .setHeader(clientIdHeader)
      .withBody(Json.toJson(updateRecord))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Done
        }
      }
}
