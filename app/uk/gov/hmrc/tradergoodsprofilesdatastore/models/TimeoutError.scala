/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models

sealed trait Error extends Throwable

case object TimeoutException extends Error {
  override def getMessage: String = "Timeout error occurred"
}

case class DownloadRequestNotFound(eori: String) extends Error {
  override def getMessage: String = s"Initial download request not found for EORI: $eori"
}

case class ConversationIdNotFound(eori: String) extends Error {
  override def getMessage: String = s"Header x-conversation-id not present in notification for EORI: $eori"
}
