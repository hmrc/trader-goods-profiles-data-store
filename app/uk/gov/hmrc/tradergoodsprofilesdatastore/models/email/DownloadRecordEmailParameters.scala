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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models.email
import play.api.libs.json.{Json, OFormat}

final case class DownloadRecordEmailParameters(
  expiredDate: String
)

object DownloadRecordEmailParameters {
  implicit val format: OFormat[DownloadRecordEmailParameters] = Json.format[DownloadRecordEmailParameters]
}

final case class DownloadRecordEmailRequest(
  to: Seq[String],
  parameters: DownloadRecordEmailParameters,
  templateId: String = DownloadRecordEmailRequest.EmailTemplateId
)

object DownloadRecordEmailRequest {
  val EmailTemplateId: String                              = "tgp_download_record_notification_email"
  implicit val format: OFormat[DownloadRecordEmailRequest] = Json.format[DownloadRecordEmailRequest]
}
