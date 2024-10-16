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

import com.google.inject.Inject
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.models.requests.IdentifierRequest

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: DataStoreAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    val predicates =
      Enrolment(config.tgpEnrolmentIdentifier.key) and (AffinityGroup.Organisation or AffinityGroup.Individual)

    authorised(predicates)
      .retrieve(Retrievals.internalId and Retrievals.affinityGroup and Retrievals.authorisedEnrolments) {
        case Some(internalId) ~ Some(affinityGroup) ~ authorisedEnrolments =>
          authorisedEnrolments
            .getEnrolment(config.tgpEnrolmentIdentifier.key)
            .flatMap(_.getIdentifier(config.tgpEnrolmentIdentifier.identifier)) match {
            case Some(enrolment) if !enrolment.value.isBlank =>
              block(IdentifierRequest(request, internalId, enrolment.value, affinityGroup))
            case _                                           => throw InsufficientEnrolments("Unable to retrieve Enrolment")
          }
        case _                                                             =>
          throw InsufficientEnrolments("Unable to retrieve Enrolment")
      } recover { case _: AuthorisationException =>
      logger.info("Authorisation failure: No enrolments found for TGP.")
      Unauthorized
    }
  }
}
