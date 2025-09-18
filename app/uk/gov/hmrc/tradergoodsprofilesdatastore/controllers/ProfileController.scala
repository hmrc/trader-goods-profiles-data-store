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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, RouterConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.ProfileRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{ProfileRepository, RecordsRepository, RecordsSummaryRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ProfileController @Inject() (
  profileRepository: ProfileRepository,
  cc: ControllerComponents,
  routerConnector: RouterConnector,
  customsDataStoreConnector: CustomsDataStoreConnector,
  recordsRepository: RecordsRepository,
  recordsSummaryRepository: RecordsSummaryRepository,
  config: DataStoreAppConfig,
  identify: IdentifierAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def setProfile: Action[ProfileRequest] = identify.async(parse.json[ProfileRequest]) { implicit request =>
    val updateOrCreateProfile =
      routerConnector.hasHistoricProfile(request.eori).flatMap { hasHistoric =>
        if (hasHistoric) routerConnector.updateTraderProfile(request.body, request.eori)
        else routerConnector.createTraderProfile(request.body, request.eori)
      }

    for {
      _ <- updateOrCreateProfile
      _ <- profileRepository.set(request.eori, request.body)
    } yield Ok
  }

  def getProfile: Action[AnyContent] = identify.async { implicit request =>
    profileRepository
      .get(request.eori)
      .map {
        case Some(profile) => Ok(Json.toJson(profile))
        case None          => NotFound
      }
  }

  def doesProfileExist: Action[AnyContent] = identify.async { implicit request =>
    profileRepository
      .get(request.eori)
      .flatMap {
        case Some(_) => Future.successful(Ok)
        case None    =>
          customsDataStoreConnector.getEoriHistory(request.eori, hc.authorization).flatMap {
            case Some(eoriHistoryResponse) =>
              val filteredEoriHistory = eoriHistoryResponse.eoriHistory.filterNot(_.eori == request.eori)
              if (filteredEoriHistory.isEmpty) {
                Future.successful(NotFound)
              } else {
                val latestEoriResult = profileRepository.get(filteredEoriHistory.head.eori).flatMap {
                  case Some(historicProfile) =>
                    for {
                      updateResult <- profileRepository.updateEori(historicProfile.eori, request.eori)
                      _            <- recordsRepository.deleteRecordsByEori(historicProfile.eori)
                      _            <- recordsSummaryRepository.deleteByEori(historicProfile.eori)
                    } yield updateResult
                  case None                  => Future.successful(false)
                }

                filteredEoriHistory.tail.map { historyItem =>
                  profileRepository.get(historyItem.eori).flatMap {
                    case Some(historyProfile) =>
                      for {
                        _ <- profileRepository.deleteByEori(historyProfile.eori)
                        _ <- recordsRepository.deleteRecordsByEori(historyProfile.eori)
                        _ <- recordsSummaryRepository.deleteByEori(historyProfile.eori)
                      } yield true
                    case None                 => Future.successful(false)
                  }
                }

                latestEoriResult.flatMap {
                  case true => Future.successful(Ok)
                  case _    => Future.successful(NotFound)
                }
              }
            case None                      => Future.successful(NotFound)
          }
      }
  }
}
