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

package uk.gov.hmrc.tradergoodsprofilesdatastore.actions

import play.api.mvc._
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.RetireFilesAction
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeRetireFilesAction @Inject() extends RetireFilesAction {
  override def filter[A](
    identifierRequest: IdentifierRequest[A]
  ): Future[Option[Result]]                                 = Future.successful(None)
  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
