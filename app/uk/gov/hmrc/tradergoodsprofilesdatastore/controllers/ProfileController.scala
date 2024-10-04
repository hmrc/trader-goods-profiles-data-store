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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, RouterConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.ProfileRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{ProfileRepository, RecordsRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ProfileController @Inject()(
                                   profileRepository: ProfileRepository,
                                   cc: ControllerComponents,
                                   routerConnector: RouterConnector,
                                   customsDataStoreConnector: CustomsDataStoreConnector,
                                   recordsRepository: RecordsRepository,
                                   config: DataStoreAppConfig,
                                   identify: IdentifierAction
                                 )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def setProfile(eori: String): Action[ProfileRequest] = identify.async(parse.json[ProfileRequest]) {
    implicit request =>
      if (config.checkForHistoricProfile) {
        routerConnector.hasHistoricProfile(eori).flatMap {
          case true =>
            routerConnector.updateTraderProfile(request.body, eori).flatMap { case Done =>
              profileRepository.set(eori, request.body).map(_ => Ok)
            }
          case false =>
            routerConnector.createTraderProfile(request.body, eori).flatMap { case Done =>
              profileRepository.set(eori, request.body).map(_ => Ok)
            }
        }
      } else {
        routerConnector.updateTraderProfile(request.body, eori).flatMap { case Done =>
          profileRepository.set(eori, request.body).map(_ => Ok)
        }
      }
  }

  def getProfile(eori: String): Action[AnyContent] = identify.async {
    profileRepository
      .get(eori)
      .map {
        case Some(profile) => Ok(Json.toJson(profile))
        case None => NotFound
      }
  }

  def doesProfileExist(eori: String): Action[AnyContent] = identify.async { implicit request =>
    profileRepository
      .get(eori)
      .flatMap {
        case Some(_) => Future.successful(Ok)
        case None =>
          customsDataStoreConnector.getEoriHistory(eori).flatMap {
            case Some(eoriHistoryResponse) =>
              if (eoriHistoryResponse.eoriHistory.isEmpty) Future.successful(NotFound)
              else {
                val latestEoriResult = profileRepository.get(eoriHistoryResponse.eoriHistory.head.eori).flatMap {
                  case Some(historicProfile) =>
                    for {
                      _ <- profileRepository.updateEori(historicProfile.eori, eori)
                      _ <- recordsRepository.deleteRecordsByEori(historicProfile.eori)
                    } yield true // How do we better track if this has succeeded?
                }

                eoriHistoryResponse.eoriHistory.tail.map { historyItem =>
                  profileRepository.get(historyItem.eori).flatMap {
                    case Some(historyProfile) =>
                      for {
                        _ <- profileRepository.deleteByEori(historyProfile.eori) // delete profile - repo method yet to implement
                        _ <- recordsRepository.deleteRecordsByEori(historyProfile.eori) // delete records
                      } yield true
                    case None => Future.successful(false)
                  }
                }

                latestEoriResult.flatMap {
                  case true => Future.successful(Ok)
                  case _ => Future.successful(NotFound)
                }
              }
            case None => Future.successful(NotFound)
          }
//                    case Some(eoriHistoryData) if eoriHistoryData.eoriHistory.isEmpty =>
//                      Future.successful(NotFound)
//
//                    case Some(eoriHistoryData) if eoriHistoryData.eoriHistory.nonEmpty =>
//                      var foundFirstProfile = false
//
//                      val updatesFutures = eoriHistoryData.eoriHistory.map { historyItem =>
//                        profileRepository.get(historyItem.eori).flatMap {
//                          case Some(historyProfile) if !foundFirstProfile =>
//                            // First profile found: update its eori and delete associated records
//                            foundFirstProfile = true
//                            for {
//                              _ <- profileRepository.updateEori(historyProfile.eori, eori) // update first matching profile
//                              _ <- recordsRepository.deleteRecordsByEori(historyProfile.eori) // delete records
//                            } yield true // track success
//
//                          case Some(historyProfile) if foundFirstProfile =>
//                            // any profiles finds after first match, delete both profile and records
//                            for {
//                              _ <- profileRepository
//                                     .deleteByEori(historyProfile.eori) // delete profile - repo method yet to implement
//                              _ <- recordsRepository.deleteRecordsByEori(historyProfile.eori) // delete records
//                            } yield true // Indicate success for deletions
//
//                          case None => Future.successful(false) // No profile found for this historyItem
//                        }
//                      }
//
//                      Future.sequence(updatesFutures).map { updates =>
//                        if (updates.contains(true)) Ok
//                        else NotFound
//                      }
//
//                    case None => Future.successful(NotFound)
      }
  }

  def deleteAll(): Action[AnyContent] = identify.async {
    if (config.droppingProfileCollection)
      profileRepository.deleteAll().map(_ => Ok)
    else
      Future.successful(InternalServerError)
  }

}
