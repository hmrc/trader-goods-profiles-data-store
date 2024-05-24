package uk.gov.hmrc.tradergoodsprofilesdatastore.connectors

import config.Service
import models.TraderProfile
import org.apache.pekko.Done
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RouterConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private val baseUrl: Service               = config.get[Service]("microservice.services.trader-goods-profiles-router")
  private def traderProfileUrl(eori: String) =
    url"$baseUrl/trader-goods-profiles-router/customs/traders/good-profiles/$eori"

  def submitTraderProfile(traderProfile: TraderProfile, eori: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .put(traderProfileUrl(eori))
      .withBody(Json.toJson(traderProfile))
      .execute[HttpResponse]
      .map(_ => Done)
}