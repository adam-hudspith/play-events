
play-events
====
[![Build Status](https://travis-ci.org/hmrc/play-events.svg?branch=master)](https://travis-ci.org/hmrc/play-events) [ ![Download](https://api.bintray.com/packages/hmrc/releases/play-events/images/download.svg) ](https://bintray.com/hmrc/releases/play-events/_latestVersion)

Play Events composes various logging and metric standards into traits which can be mixed in to create events for your play app.

Auditing, Metrics, Alerts or Logging data can be recorded. A default monitoring implementation for HTTP errors is also provided.

##Download play-events

```scala
libraryDependencies += "uk.gov.hmrc" %% "play-events" % "0.4.0"
```

##Creating an Alert Event

A basic Alert Event would look like this:

```scala
package uk.gov.hmrc.play.events.examples

import uk.gov.hmrc.play.events.AlertLevel._
import uk.gov.hmrc.play.events.Alertable

case class ExampleAlertEvent(source: String,
                             name: String,
                             level: AlertLevel) extends Alertable

object ExampleAlertEvent {
  def apply() = new ExampleAlertEvent(
    source = "TestApp",
    name = "External API Alert",
    level = CRITICAL
  )
}
```

##Recording an Alert Event

The above event would be recorded in your app by mixing in the `DefaultEventRecorder` trait and calling:

```scala
record(ExampleAlertEvent())
```

Alert Events are written out to the logs in the following standard format. Match on this format with your Paging or Alerts service.
```scala
Logger.warn(s"alert:${alertable.level}:source:${alertable.source}" + 
             ":name:${alertable.name}")
```

So the output of this alert will be similar to 
```2015-08-04 13:54:56,793 level=[WARN] logger=[application] message=[alert:CRITICAL:source:TestApp:name:External Api Alert]```

##Creating an Audit Event

Audit Events look the same as any other event except they have ```privateData``` in place of ```data```. 
Use this paramater to store sensitive data which should be written out to a secure events log. 

They also typically take in an ```implicit HeaderCarrier``` in order to log the user session.

```scala
package uk.gov.hmrc.play.events.examples

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.events.Auditable

case class ExampleAuditEvent(source: String,
                             name: String,
                             tags: Map[String, String],
                             privateData: Map[String, String]) extends Auditable

object ExampleAuditEvent {

  def apply(testCount: Int, testName: String)(implicit hc: HeaderCarrier) =
    ExampleAuditEvent(
      source = "example-source",
      name = "test-conducted",
      tags = Map(hc.toAuditTags("testConducted", 
                                "/your-web-app/example-path/").toSeq: _*),
      privateData = hc.toAuditDetails() ++ buildAuditData(testCount, testName)
    )

  private def buildAuditData(count: Int, name: String) = {
      Map(
        "Test Name" -> name.toString,
        "Tests Run" -> count.toString
      )
  }
}
```

##Recording an Audit Event

Audit Events require a ```AuditConnector``` to be set up inside your application to register events with. 
This should be registered inside a ```AuditEventHandler```. 

```scala
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.events.handlers.AuditEventHandler

object DefaultAuditEventHandler extends AuditEventHandler {
  override val auditConnector: AuditConnector = AuditConnector
}
```

You then add this Handler to your own ```Recorder``` which you then include in your app where ever you want to audit.
Note that we include ```super.eventHandlers``` so that the Default Alert, Metric and Loggable Handlers are included.

```scala
trait ExampleEventRecorder extends DefaultEventRecorder {
  override def eventHandlers: Set[EventHandler] = 
    super.eventHandlers ++ Set(DefaultAuditEventHandler)
}
```

And now you record the Audit Event as normal
```scala
record(ExampleAuditEvent(5, "Example"))
```

##Creating a Metric Event

A Metric Event currently writes out to the log in a standard format, which can be analysed in your Log Digestion app 
to create reports etc.

```scala
package uk.gov.hmrc.play.events.examples

import uk.gov.hmrc.play.events.Measurable

case class ExampleMetricEvent(source: String,
                              name: String,
                              data: Map[String, String]) extends Measurable

object ExampleMetricEvent {
  def apply(fileId: String, fileType: String) =
    new ExampleMetricEvent(
      source = "TestApp",
      name = "NumberOfCreatedFilings",
      data = Map (
      "File ID" -> fileId,
      "File Type" -> fileType
    ))
}
```

##Recording a Metric Event

The ```DefaultEventRecorder``` trait writes out Metric Events in this standard format.

```scala
Logger.info(s"metric:source:${measurable.source}:name:${measurable.name}" + 
                ":data:${measurable.data}")
```

They can be recorded by:

```scala
record(ExampleMetricEvent("1234567", "SHARED"))
```

The output of this metric will be similar to 
```2015-08-04 13:54:56,793 level=[INFO] logger=[application] message=[metric:source:TestApp:name:NumberOfCreatedFilings:data:Map(File ID -> 1234567, File Type -> SHARED]```

##Combining Event Types in One Event

The event traits can be mixed in together to allow for an event to be recorded once but produce multiple types of 
events in your logs. 

Note the separation between ```privateData``` and ```data``` for AuditEvents. This also uses the ```Loggable``` trait to
add an additional message into the logs.

```scala
package uk.gov.hmrc.play.events.examples

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.events.AlertLevel.AlertLevel
import uk.gov.hmrc.play.events._

case class ExampleCombinedEvent(source: String,
                                name: String,
                                tags: Map[String, String],
                                privateData: Map[String, String],
                                data: Map[String, String],
                                level: AlertLevel) 
            extends Auditable with Measurable with Loggable with Alertable {

 override def log = "Combined Event occurred"

}

object ExampleCombinedEvent {

  def apply(filingID: String, otherFilingInfo: String, userPassword: String)
           (implicit hc: HeaderCarrier) = 
  new ExampleCombinedEvent(
    source = "test-app",
    name = "CombinedEvent",
    tags = Map(hc.toAuditTags("testConducted", 
                              "/your-web-app/example-path/").toSeq: _*),
    privateData = Map("Password" -> userPassword) ++ 
                  generateData(filingID, otherFilingInfo),
    data = hc.toAuditDetails() ++ generateData(filingID, otherFilingInfo),
    AlertLevel.WARNING
  )

  def generateData(filingID: String, otherFilingInfo: String): Map[String, String] =
    Map("Filing ID" -> filingID, "Filing Info" -> otherFilingInfo)
}
```

This event can be recorded using the ```ExampleEventRecorder``` above.

##Default Monitoring of Http Errors

The trait ```HttpMonitor``` provides a default monitoring solution for any app that uses the **uk.gov.hmrc.http-verbs**
library. In any class that extends ```HttpMonitor``` you can wrap your code with ```monitor``` as follows:

```scala
def getHttpData()(implicit hc: HeaderCarrier) : Future[ExampleHttpResponse] = {
    monitor {
      http.GET[ExampleHttpResponse](exampleGetUrl)
    }
}
```

This will catch any of ```uk.gov.hmrc.play.http.{HttpException, Upstream4xxResponse, Upstream5xxResponse}``` and log 
both a Metric and Critical Alert event for any that occur, and pass the exception along.
