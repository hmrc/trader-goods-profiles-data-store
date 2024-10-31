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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{atLeastOnce, never, times, verify, when}
import org.mockito.MockitoSugar.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.inject
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.tradergoodsprofilesdatastore.actions.FakeIdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.connectors.{CustomsDataStoreConnector, RouterConnector}
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.IdentifierAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.ProfileRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.response.{EoriHistoricItem, EoriHistoryResponse, ProfileResponse}
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.{ProfileRepository, RecordsRepository, RecordsSummaryRepository}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class ProfileControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val requestEori = "GB123456789099"

  private val profileRequest = ProfileRequest(
    actorId = "GB123456789099",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = Some("RMS-GB-123456"),
    niphlNumber = Some("6 S12345")
  )

  private val setUrl                = routes.ProfileController.setProfile(requestEori).url
  private val getUrl                = routes.ProfileController.getProfile(requestEori).url
  private val doesExistUrl          = routes.ProfileController.doesProfileExist(requestEori).url
  private val validFakePutRequest   =
    FakeRequest("PUT", setUrl, FakeHeaders(Seq(CONTENT_TYPE -> JSON)), Json.toJson(profileRequest))
  private val invalidFakePutRequest = FakeRequest("PUT", setUrl, FakeHeaders(Seq(CONTENT_TYPE -> JSON)), "{}")
  private val validFakeGetRequest   = FakeRequest("GET", getUrl)
  private val validDoesExistRequest = FakeRequest("HEAD", doesExistUrl)

  val mockRouterConnector: RouterConnector                    = mock[RouterConnector]
  val mockCustomDataStoreConnector: CustomsDataStoreConnector = mock[CustomsDataStoreConnector]
  val mockProfileRepository: ProfileRepository                = mock[ProfileRepository]
  val mockRecordsRepository: RecordsRepository                = mock[RecordsRepository]
  val mockRecordsSummaryRepository: RecordsSummaryRepository  = mock[RecordsSummaryRepository]
  val dataStoreAppConfig: DataStoreAppConfig                  = mock[DataStoreAppConfig]

  private val expectedProfileResponse = ProfileResponse(
    eori = "1234567890",
    actorId = "1234567890",
    ukimsNumber = "XIUKIM47699357400020231115081800",
    nirmsNumber = Some("123456"),
    niphlNumber = Some("123456"),
    eoriChanged = Some(false)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockRouterConnector)
    reset(mockCustomDataStoreConnector)
    reset(mockProfileRepository)
    reset(mockRecordsRepository)
    reset(mockRecordsSummaryRepository)
  }

  s"PUT $setUrl" - {

    "when the checkForHistoricProfile flag is set to true" - {

      "call update profile when historic profile exists, update repository and return 200 when valid data is posted" in {
        when(dataStoreAppConfig.checkForHistoricProfile) thenReturn true
        when(mockProfileRepository.set(any(), any())) thenReturn Future.successful(true)
        when(mockRouterConnector.hasHistoricProfile(any())(any())) thenReturn Future.successful(true)
        when(mockRouterConnector.updateTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)
        val application = applicationBuilder()
          .overrides(
            inject.bind[ProfileRepository].toInstance(mockProfileRepository),
            inject.bind[RouterConnector].toInstance(mockRouterConnector),
            inject.bind[DataStoreAppConfig].toInstance(dataStoreAppConfig)
          )
          .build()

        running(application) {
          val result = route(application, validFakePutRequest).value
          status(result) shouldBe Status.OK

          verify(mockProfileRepository, times(1)).set(any(), any())
          verify(mockRouterConnector, times(1)).hasHistoricProfile(any())(any())
          verify(mockRouterConnector, times(1)).updateTraderProfile(any(), any())(any())
        }
      }

      "call create profile when historic profile does not exist, update repository and return 200 when valid data is posted" in {
        when(dataStoreAppConfig.checkForHistoricProfile) thenReturn true
        when(mockProfileRepository.set(any(), any())) thenReturn Future.successful(true)
        when(mockRouterConnector.hasHistoricProfile(any())(any())) thenReturn Future.successful(false)
        when(mockRouterConnector.createTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)
        val application = applicationBuilder()
          .overrides(
            inject.bind[ProfileRepository].toInstance(mockProfileRepository),
            inject.bind[RouterConnector].toInstance(mockRouterConnector),
            inject.bind[DataStoreAppConfig].toInstance(dataStoreAppConfig)
          )
          .build()

        running(application) {
          val result = route(application, validFakePutRequest).value
          status(result) shouldBe Status.OK

          verify(mockProfileRepository, times(1)).set(any(), any())
          verify(mockRouterConnector, times(1)).hasHistoricProfile(any())(any())
          verify(mockRouterConnector, times(1)).createTraderProfile(any(), any())(any())
        }
      }
    }

    "when the checkForHistoricProfile flag is set to false" - {
      "call update profile, update repository and return 200 when valid data is posted" in {
        when(dataStoreAppConfig.checkForHistoricProfile) thenReturn false
        when(mockProfileRepository.set(any(), any())) thenReturn Future.successful(true)
        when(mockRouterConnector.updateTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)
        val application = applicationBuilder()
          .overrides(
            inject.bind[ProfileRepository].toInstance(mockProfileRepository),
            inject.bind[RouterConnector].toInstance(mockRouterConnector),
            inject.bind[DataStoreAppConfig].toInstance(dataStoreAppConfig)
          )
          .build()

        running(application) {
          val result = route(application, validFakePutRequest).value
          status(result) shouldBe Status.OK

          verify(mockProfileRepository, times(1)).set(any(), any())
          verify(mockRouterConnector, times(1)).updateTraderProfile(any(), any())(any())
        }
      }
    }

    "return 400 when invalid data is posted" in {
      when(mockProfileRepository.set(any(), any())) thenReturn Future.successful(true)
      when(mockRouterConnector.updateTraderProfile(any(), any())(any())) thenReturn Future.successful(Done)
      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository),
          inject.bind[IdentifierAction].to[FakeIdentifierAction]
        )
        .build()

      running(application) {
        val result = route(application, invalidFakePutRequest).value
        status(result) shouldBe Status.BAD_REQUEST

        verify(mockProfileRepository, never()).set(any(), any())
        verify(mockRouterConnector, never()).updateTraderProfile(any(), any())(any())
      }
    }
  }

  s"GET $getUrl" - {

    "return 200 when data is found" in {
      when(mockProfileRepository.get(any())) thenReturn Future.successful(Some(expectedProfileResponse))

      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.OK

        contentAsString(result) mustBe Json.toJson(expectedProfileResponse).toString

        verify(mockProfileRepository, atLeastOnce()).get(any())
      }
    }

    "return 404 when data is not found" in {

      when(mockProfileRepository.get(any())) thenReturn Future.successful(None)

      val application = applicationBuilder()
        .overrides(
          inject.bind[ProfileRepository].toInstance(mockProfileRepository)
        )
        .build()

      running(application) {
        val result = route(application, validFakeGetRequest).value
        status(result) shouldBe Status.NOT_FOUND

        verify(mockProfileRepository, atLeastOnce()).get(any())
      }
    }

  }

  s"HEAD $doesExistUrl" - {

    "return 200" - {

      "when initial profile exists" in {
        when(mockProfileRepository.get(any())) thenReturn Future.successful(Some(expectedProfileResponse))

        val application = applicationBuilder()
          .overrides(
            inject.bind[ProfileRepository].toInstance(mockProfileRepository)
          )
          .build()

        running(application) {
          val result = route(application, validDoesExistRequest).value
          status(result) shouldBe Status.OK

          verify(mockProfileRepository, atLeastOnce()).get(any())
        }
      }

      "when initial profile does not exist" - {

        "and historic eori data does exist and older historic profiles not found" in {
          when(mockProfileRepository.get(eqTo(requestEori))) thenReturn Future.successful(None)
          when(mockCustomDataStoreConnector.getEoriHistory(any())(any())) thenReturn Future.successful(
            Some(
              EoriHistoryResponse(
                Seq(
                  EoriHistoricItem(
                    requestEori,
                    Instant.parse("2024-04-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  ),
                  EoriHistoricItem(
                    "previousEori",
                    Instant.parse("2024-03-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  )
                )
              )
            )
          )

          when(mockProfileRepository.updateEori(any(), any())) thenReturn Future.successful(true)
          when(mockRecordsRepository.deleteRecordsByEori(any())) thenReturn Future.successful(1)
          when(mockRecordsSummaryRepository.deleteByEori(any())) thenReturn Future.successful(1)

          val historicEoriProfile = ProfileResponse(
            eori = "previousEori",
            actorId = "previousEori",
            ukimsNumber = "XIUKIM47699357400020231115081800",
            nirmsNumber = Some("RMS-GB-123456"),
            niphlNumber = Some("6 S12345"),
            eoriChanged = Some(false)
          )

          when(mockProfileRepository.get(eqTo("previousEori"))) thenReturn Future.successful(Some(historicEoriProfile))

          val application = applicationBuilder()
            .overrides(
              inject.bind[ProfileRepository].toInstance(mockProfileRepository),
              inject.bind[CustomsDataStoreConnector].toInstance(mockCustomDataStoreConnector),
              inject.bind[RecordsRepository].toInstance(mockRecordsRepository),
              inject.bind[RecordsSummaryRepository].toInstance(mockRecordsSummaryRepository)
            )
            .build()

          running(application) {
            val result = route(application, validDoesExistRequest).value
            status(result) shouldBe Status.OK

            withClue("must call update eori and delete records") {
              verify(mockProfileRepository, atLeastOnce()).get(eqTo(requestEori))
              verify(mockCustomDataStoreConnector, atLeastOnce()).getEoriHistory(any())(any())
              verify(mockProfileRepository, atLeastOnce()).updateEori(any, any)
              verify(mockRecordsRepository, atLeastOnce()).deleteRecordsByEori(any)
              verify(mockRecordsSummaryRepository, atLeastOnce()).deleteByEori(any)
            }
          }
        }

        "and historic eori data does exist and should delete older historic profiles" in {
          when(mockProfileRepository.get(eqTo(requestEori))) thenReturn Future.successful(None)
          when(mockCustomDataStoreConnector.getEoriHistory(any())(any())) thenReturn Future.successful(
            Some(
              EoriHistoryResponse(
                Seq(
                  EoriHistoricItem(
                    requestEori,
                    Instant.parse("2024-05-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  ),
                  EoriHistoricItem(
                    "previousEori",
                    Instant.parse("2024-04-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  ),
                  EoriHistoricItem(
                    "eori2",
                    Instant.parse("2024-03-20T00:00:00Z"),
                    Instant.parse("2024-09-20T00:00:00Z")
                  )
                )
              )
            )
          )

          val historicEoriProfile = ProfileResponse(
            eori = "previousEori",
            actorId = "previousEori",
            ukimsNumber = "XIUKIM47699357400020231115081800",
            nirmsNumber = Some("RMS-GB-123456"),
            niphlNumber = Some("6 S12345"),
            eoriChanged = Some(false)
          )

          when(mockProfileRepository.get(eqTo("previousEori"))) thenReturn Future.successful(Some(historicEoriProfile))
          when(mockProfileRepository.updateEori(any(), any())) thenReturn Future.successful(true)
          when(mockRecordsRepository.deleteRecordsByEori(eqTo("previousEori"))) thenReturn Future.successful(1)
          when(mockRecordsSummaryRepository.deleteByEori(eqTo("previousEori"))) thenReturn Future.successful(1)

          val olderHistoricEoriProfile = ProfileResponse(
            eori = "eori2",
            actorId = "eori2",
            ukimsNumber = "XIUKIM47699357400020231115081800",
            nirmsNumber = Some("RMS-GB-123456"),
            niphlNumber = Some("6 S12345"),
            eoriChanged = Some(false)
          )

          when(mockProfileRepository.get(eqTo("eori2"))) thenReturn Future.successful(Some(olderHistoricEoriProfile))
          when(mockProfileRepository.deleteByEori(eqTo("eori2"))) thenReturn Future.successful(1)
          when(mockRecordsRepository.deleteRecordsByEori(eqTo("eori2"))) thenReturn Future.successful(1)
          when(mockRecordsSummaryRepository.deleteByEori(eqTo("eori2"))) thenReturn Future.successful(1)

          val application = applicationBuilder()
            .overrides(
              inject.bind[ProfileRepository].toInstance(mockProfileRepository),
              inject.bind[CustomsDataStoreConnector].toInstance(mockCustomDataStoreConnector),
              inject.bind[RecordsRepository].toInstance(mockRecordsRepository),
              inject.bind[RecordsSummaryRepository].toInstance(mockRecordsSummaryRepository)
            )
            .build()

          running(application) {
            val result = route(application, validDoesExistRequest).value
            status(result) shouldBe Status.OK

            withClue("must called delete records twice and delete eori profile once") {
              verify(mockProfileRepository, atLeastOnce()).updateEori(any, any)
              verify(mockRecordsRepository, times(2)).deleteRecordsByEori(any)
              verify(mockProfileRepository, atLeastOnce()).deleteByEori(any)
              verify(mockRecordsSummaryRepository, times(2)).deleteByEori(any)
            }
          }
        }

        "and historic eori data does exist and does not delete older historic profiles" in {
          when(mockProfileRepository.get(eqTo(requestEori))) thenReturn Future.successful(None)
          when(mockCustomDataStoreConnector.getEoriHistory(any())(any())) thenReturn Future.successful(
            Some(
              EoriHistoryResponse(
                Seq(
                  EoriHistoricItem(
                    requestEori,
                    Instant.parse("2024-05-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  ),
                  EoriHistoricItem(
                    "previousEori",
                    Instant.parse("2024-04-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  ),
                  EoriHistoricItem(
                    "eori2",
                    Instant.parse("2024-03-20T00:00:00Z"),
                    Instant.parse("2024-09-20T00:00:00Z")
                  )
                )
              )
            )
          )

          when(mockProfileRepository.updateEori(any(), any())) thenReturn Future.successful(true)
          when(mockRecordsRepository.deleteRecordsByEori(eqTo("previousEori"))) thenReturn Future.successful(1)
          when(mockRecordsSummaryRepository.deleteByEori(eqTo("previousEori"))) thenReturn Future.successful(1)

          val historicEoriProfile = ProfileResponse(
            eori = "previousEori",
            actorId = "previousEori",
            ukimsNumber = "XIUKIM47699357400020231115081800",
            nirmsNumber = Some("RMS-GB-123456"),
            niphlNumber = Some("6 S12345"),
            eoriChanged = Some(false)
          )

          when(mockProfileRepository.get(eqTo("previousEori"))) thenReturn Future.successful(Some(historicEoriProfile))
          when(mockProfileRepository.get(eqTo("eori2"))) thenReturn Future.successful(None)

          val application = applicationBuilder()
            .overrides(
              inject.bind[ProfileRepository].toInstance(mockProfileRepository),
              inject.bind[CustomsDataStoreConnector].toInstance(mockCustomDataStoreConnector),
              inject.bind[RecordsRepository].toInstance(mockRecordsRepository),
              inject.bind[RecordsSummaryRepository].toInstance(mockRecordsSummaryRepository)
            )
            .build()

          running(application) {
            val result = route(application, validDoesExistRequest).value
            status(result) shouldBe Status.OK

            withClue("must called delete records and delete eori profile once") {
              verify(mockProfileRepository, times(3)).get(any)
              verify(mockProfileRepository, atLeastOnce()).updateEori(any, any)
              verify(mockRecordsRepository, atLeastOnce()).deleteRecordsByEori(any)
              verify(mockRecordsSummaryRepository, atLeastOnce()).deleteByEori(any)
            }
          }
        }
      }
    }

    "return 404" - {
      "when initial profile does not exist" - {

        "and historic eori data does not exist" in {
          when(mockProfileRepository.get(any())) thenReturn Future.successful(None)
          when(mockCustomDataStoreConnector.getEoriHistory(any())(any())) thenReturn Future.successful(
            Some(
              EoriHistoryResponse(
                Seq(
                  EoriHistoricItem(
                    requestEori,
                    Instant.parse("2024-04-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  )
                )
              )
            )
          )

          val application = applicationBuilder()
            .overrides(
              inject.bind[ProfileRepository].toInstance(mockProfileRepository),
              inject.bind[CustomsDataStoreConnector].toInstance(mockCustomDataStoreConnector)
            )
            .build()

          running(application) {
            val result = route(application, validDoesExistRequest).value
            status(result) shouldBe Status.NOT_FOUND

            verify(mockProfileRepository, atLeastOnce()).get(any())
            verify(mockCustomDataStoreConnector, atLeastOnce()).getEoriHistory(any())(any())
          }
        }

        "and historic eori data not found" in {
          when(mockProfileRepository.get(any())) thenReturn Future.successful(None)
          when(mockCustomDataStoreConnector.getEoriHistory(any())(any())) thenReturn Future.successful(None)

          val application = applicationBuilder()
            .overrides(
              inject.bind[ProfileRepository].toInstance(mockProfileRepository),
              inject.bind[CustomsDataStoreConnector].toInstance(mockCustomDataStoreConnector)
            )
            .build()

          running(application) {
            val result = route(application, validDoesExistRequest).value
            status(result) shouldBe Status.NOT_FOUND

            verify(mockProfileRepository, atLeastOnce()).get(any())
            verify(mockCustomDataStoreConnector, atLeastOnce()).getEoriHistory(any())(any())
          }
        }

        "and eori history does exist, but latest historical eori profile does not exist" in {
          when(mockProfileRepository.get(eqTo(requestEori))) thenReturn Future.successful(None)
          when(mockCustomDataStoreConnector.getEoriHistory(any())(any())) thenReturn Future.successful(
            Some(
              EoriHistoryResponse(
                Seq(
                  EoriHistoricItem(
                    requestEori,
                    Instant.parse("2024-04-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  ),
                  EoriHistoricItem(
                    "previousEori",
                    Instant.parse("2024-04-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  )
                )
              )
            )
          )

          when(mockProfileRepository.get(eqTo("previousEori"))) thenReturn Future.successful(None)

          val application = applicationBuilder()
            .overrides(
              inject.bind[ProfileRepository].toInstance(mockProfileRepository),
              inject.bind[CustomsDataStoreConnector].toInstance(mockCustomDataStoreConnector)
            )
            .build()

          running(application) {
            val result = route(application, validDoesExistRequest).value
            status(result) shouldBe Status.NOT_FOUND

            verify(mockProfileRepository, times(2)).get(any())
            verify(mockCustomDataStoreConnector, atLeastOnce()).getEoriHistory(any())(any())
          }
        }

        "and updating historic eori with the new eori fails" in {
          when(mockProfileRepository.get(eqTo(requestEori))) thenReturn Future.successful(None)
          when(mockCustomDataStoreConnector.getEoriHistory(any())(any())) thenReturn Future.successful(
            Some(
              EoriHistoryResponse(
                Seq(
                  EoriHistoricItem(
                    requestEori,
                    Instant.parse("2024-04-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  ),
                  EoriHistoricItem(
                    "previousEori",
                    Instant.parse("2024-04-20T00:00:00Z"),
                    Instant.parse("2024-10-20T00:00:00Z")
                  )
                )
              )
            )
          )

          val historicEoriProfile = ProfileResponse(
            eori = "previousEori",
            actorId = "previousEori",
            ukimsNumber = "XIUKIM47699357400020231115081800",
            nirmsNumber = Some("RMS-GB-123456"),
            niphlNumber = Some("6 S12345"),
            eoriChanged = Some(false)
          )

          when(mockProfileRepository.get(eqTo("previousEori"))) thenReturn Future.successful(Some(historicEoriProfile))
          when(mockProfileRepository.updateEori(any(), any())) thenReturn Future.successful(false)
          when(mockRecordsRepository.deleteRecordsByEori(any())) thenReturn Future.successful(0)

          val application = applicationBuilder()
            .overrides(
              inject.bind[ProfileRepository].toInstance(mockProfileRepository),
              inject.bind[CustomsDataStoreConnector].toInstance(mockCustomDataStoreConnector),
              inject.bind[RecordsRepository].toInstance(mockRecordsRepository)
            )
            .build()

          running(application) {
            val result = route(application, validDoesExistRequest).value
            status(result) shouldBe Status.NOT_FOUND

            verify(mockProfileRepository, times(2)).get(any())
            verify(mockProfileRepository, atLeastOnce()).updateEori(any(), any())
            verify(mockRecordsRepository, atLeastOnce()).deleteRecordsByEori(any())
            verify(mockCustomDataStoreConnector, atLeastOnce()).getEoriHistory(any())(any())
          }
        }
      }
    }
  }
}
