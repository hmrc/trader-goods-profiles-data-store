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
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.Service
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.email.{DownloadRecordEmailParameters, DownloadRecordEmailRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val emailServiceBaseUrl: Service = config.get[Service]("microservice.services.email")

  private def sendEmailUrl = url"$emailServiceBaseUrl/hmrc/email"

  def sendDownloadRecordEmail(to: String, downloadRecordEmailParameters: DownloadRecordEmailParameters)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    httpClient
      .post(sendEmailUrl)
      .withBody(Json.toJson(DownloadRecordEmailRequest(Seq(to), downloadRecordEmailParameters)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case ACCEPTED => Future.successful(Done)
          case _        => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
}
