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

package uk.gov.hmrc.tradergoodsprofilesdatastore.models

import play.api.libs.json._

sealed abstract class DownloadDataStatus(val value: String)

object DownloadDataStatus {
  final case object FileInProgress extends DownloadDataStatus("FileInProgress")

  final case object FileReadySeen extends DownloadDataStatus("FileReadySeen")

  final case object FileReadyUnseen extends DownloadDataStatus("FileReadyUnseen")

  lazy val downloadDataStatusTypes: Set[DownloadDataStatus] =
    Set(FileInProgress, FileReadySeen, FileReadyUnseen)

  private def enumFormat[A](values: Set[A])(getKey: A => String): Format[A] = new Format[A] {

    override def writes(a: A): JsValue =
      JsString(getKey(a))

    override def reads(json: JsValue): JsResult[A] = json match {
      case JsString(str) =>
        values
          .find(getKey(_) == str)
          .map(JsSuccess(_))
          .getOrElse(JsError("error.expected.validenumvalue"))
      case _             =>
        JsError("error.expected.enumstring")
    }
  }

  implicit val format: Format[DownloadDataStatus] =
    enumFormat(DownloadDataStatus.downloadDataStatusTypes)(_.value)
}
