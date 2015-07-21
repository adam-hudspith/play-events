/*
 * Copyright 2015 HM Revenue & Customs
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

package uk.gov.hmrc.play.events

import play.api.Logger

object DefaultAlertEventHandler extends AlertEventHandler {
  override def handleAlertable(alertable: Alertable) = Logger.info(s"alert:${alertable.level}:team:${alertable.team}:source:${alertable.source}:name:${alertable.name}:data:${alertable.data}")
}

trait AlertEventHandler extends EventHandler {

  def handleAlertable(alertable: Alertable)

  override def handle(event: Recordable): Unit = event match {
    case alertable: Alertable => handleAlertable(alertable)
    case _ =>
  }

}
