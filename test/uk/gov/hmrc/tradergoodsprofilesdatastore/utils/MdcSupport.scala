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

package uk.gov.hmrc.tradergoodsprofilesdatastore.utils


import org.slf4j.MDC
import uk.gov.hmrc.http.client.Executor
import java.util.concurrent.Executor // Import the correct Executor

import scala.concurrent.{ExecutionContext, Future}

object MdcSupport {

  def mdcExecutionContext(ec: ExecutionContext): ExecutionContext = {
    val contextMap = Option(MDC.getCopyOfContextMap)

    ExecutionContext.fromExecutor(new java.util.concurrent.Executor {
      override def execute(runnable: Runnable): Unit = {
        ec.execute(new Runnable {
          override def run(): Unit = {
            contextMap match {
              case Some(map) => MDC.setContextMap(map)
              case None => MDC.clear()
            }
            try {
              runnable.run()
            } finally {
              MDC.clear()
            }
          }
        })
      }
    })
  }

  def withMdc[A](futureBlock: => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    implicit val mdcEc: ExecutionContext = mdcExecutionContext(ec)
    futureBlock
  }
}



