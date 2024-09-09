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

import play.api.Configuration
import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.Service
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{DownloadData, Email}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SecureDataExchangeProxyConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val baseSecureDataExchangeProxy: Service =
    config.get[Service]("microservice.services.secure-data-exchange-proxy")

  private val informationType = "placeholder"
  private val serverToken     = "placeholder"

  private def headers(eori: String) =
    Seq("x-client-id" -> serverToken, "X-SDES-Key" -> eori)
  private def filesAvailableUrl()   =
    url"$baseSecureDataExchangeProxy/secure-data-exchange-proxy/files-available/list/$informationType"

  def getFilesAvailableUrl(
    eori: String
  )(implicit hc: HeaderCarrier): Future[Seq[DownloadData]] =
    httpClient
      .get(filesAvailableUrl())
      .setHeader(headers(eori): _*)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(response.json.as[Seq[DownloadData]])
          case _  => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
}
