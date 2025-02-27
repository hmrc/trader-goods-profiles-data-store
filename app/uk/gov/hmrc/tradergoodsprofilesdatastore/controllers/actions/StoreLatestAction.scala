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

package uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.Accepted
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.TimeoutException
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest
import uk.gov.hmrc.tradergoodsprofilesdatastore.repositories.RecordsSummaryRepository
import uk.gov.hmrc.tradergoodsprofilesdatastore.services.StoreRecordsService

import javax.inject.Inject
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

class StoreLatestActionImpl @Inject() (
  recordsSummaryRepository: RecordsSummaryRepository,
  storeRecordsService: StoreRecordsService
)(implicit val executionContext: ExecutionContext)
    extends StoreLatestAction
    with Logging {

  private def withTimeout[T](future: Future[T], timeout: FiniteDuration): Future[T] = {
    implicit val system: ActorSystem = ActorSystem("TimeoutSystem")
    val timeoutFuture                = after(timeout, system.scheduler)(Future.failed(TimeoutException))
    Future.firstCompletedOf(Seq(future, timeoutFuture))
  }

  override protected def filter[A](
    identifierRequest: IdentifierRequest[A]
  ): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(identifierRequest)

    val result = withTimeout(
      recordsSummaryRepository.get(identifierRequest.eori).flatMap { recordsSummaryOpt =>
        storeRecordsService
          .storeRecords(
            identifierRequest.eori,
            recordsSummaryOpt.map(_.lastUpdated.toString)
          )
          .map { isDone =>
            if (isDone) {
              None
            } else {
              Some(Accepted)
            }
          }
      },
      1.seconds
    ).recoverWith { case TimeoutException =>
      Future.successful(Some(Accepted))
    }

    result
  }
}

trait StoreLatestAction extends ActionFilter[IdentifierRequest]
