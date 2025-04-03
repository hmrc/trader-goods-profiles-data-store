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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.Service
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{Email, EoriHistoryResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomsDataStoreConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val baseUrlCustomsDataStore: Service = config.get[Service]("microservice.services.customs-data-store")
  private val stubbedCustomsDataStore: Service = config.get[Service]("microservice.services.stubbed-customs-data-store")
  private val stubVerifiedEmail: Boolean       = config.get[Boolean]("features.stub-verified-email")
  private val isCDSMigrationEnabled: Boolean   = config.get[Boolean]("features.cds-migration")

  private def customsDataStoreBaseUrl: String =
    if (stubVerifiedEmail) stubbedCustomsDataStore else baseUrlCustomsDataStore

  private def emailUrl(eori: String) =
    if (isCDSMigrationEnabled) {
      url"$customsDataStoreBaseUrl/customs-data-store/eori/verified-email-third-party"
    } else {
      url"$customsDataStoreBaseUrl/customs-data-store/eori/$eori/verified-email"
    }

  private def eoriHistoryUrl(eori: String) = if (isCDSMigrationEnabled) {
    url"$baseUrlCustomsDataStore/customs-data-store/eori/eori-history"
  } else {
    url"$baseUrlCustomsDataStore/customs-data-store/eori/$eori/eori-history"
  }

  def getEmailViaPost(
    eori: String
  )(implicit hc: HeaderCarrier): Future[Option[Email]] = {

    val json: JsValue = Json.obj(
      "eori" -> eori
    )

    httpClient
      .post(emailUrl(eori))
      .withBody(json)
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK        => Future.successful(Some(response.json.as[Email]))
          case NOT_FOUND => Future.successful(None)
          case _         => Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
      }
  }

  def getEmail(
    eori: String
  )(implicit hc: HeaderCarrier): Future[Option[Email]] =
    if (isCDSMigrationEnabled) {
      getEmailViaPost(eori)
    } else {
      httpClient
        .get(emailUrl(eori))
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK        => Future.successful(Some(response.json.as[Email]))
            case NOT_FOUND => Future.successful(None)
            case _         => Future.failed(UpstreamErrorResponse(response.body, response.status))
          }
        }
    }

  def getEoriHistory(
    eori: String,
    authorisationToken: Option[Authorization] = None
  )(implicit hc: HeaderCarrier): Future[Option[EoriHistoryResponse]] = {

    val http: RequestBuilder = authorisationToken match {
      case Some(token) if isCDSMigrationEnabled =>
        httpClient.get(eoriHistoryUrl(eori)).setHeader(("Authorization", s"Bearer ${token.value}"))
      case _                                    => httpClient.get(eoriHistoryUrl(eori))
    }

    http.execute[HttpResponse].flatMap { response =>
      response.status match {
        case OK        => Future.successful(Some(response.json.as[EoriHistoryResponse]))
        case NOT_FOUND => Future.successful(None)
        case _         => Future.failed(UpstreamErrorResponse(response.body, response.status))
      }
    }
  }
}
