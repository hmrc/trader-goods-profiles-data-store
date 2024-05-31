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

import com.google.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofilesdatastore.base.SpecBase
import uk.gov.hmrc.tradergoodsprofilesdatastore.config.DataStoreAppConfig
import uk.gov.hmrc.tradergoodsprofilesdatastore.controllers.actions.{AuthenticatedIdentifierAction, IdentifierAction}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AuthActionSpec extends SpecBase {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Results.Ok)
  }

  "Auth Action" - {

    "must return unauthorized when the API request is missing the bearer token" in {

      val application = applicationBuilder().build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
        val appConfig   = application.injector.instanceOf[DataStoreAppConfig]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new MissingBearerToken),
          appConfig,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "must return unauthorized when the API request has expired the bearer token" in {

      val application = applicationBuilder().build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
        val appConfig   = application.injector.instanceOf[DataStoreAppConfig]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new BearerTokenExpired),
          appConfig,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "must return unauthorized when the API request has insufficient enrolments" in {

      val application = applicationBuilder().build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
        val appConfig   = application.injector.instanceOf[DataStoreAppConfig]

        val authAction = new AuthenticatedIdentifierAction(
          new FakeFailingAuthConnector(new InsufficientEnrolments),
          appConfig,
          bodyParsers
        )
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }
    }
  }

}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] =
    Future.failed(exceptionToReturn)
}

class FakeSuccessfulAuthConnector @Inject() (eori: String) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[A] = {
    val authResponse = new ~(
      new ~(Some("internalId"), Some(AffinityGroup.Individual)),
      Enrolments(Set(Enrolment("HMRC-CUS-ORG", Seq(EnrolmentIdentifier("EORINumber", eori)), "")))
    )

    Future.fromTry(Try(authResponse.asInstanceOf[A]))
  }
}
