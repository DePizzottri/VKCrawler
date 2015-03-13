package com.vkcrawler.DataModel.SprayJsonSupport

import spray.json._
import spray.json.DefaultJsonProtocol
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import spray.httpx.SprayJsonSupport
import com.vkcrawler.DataModel._
import java.util.Date

object JodaDateTimeSupport extends DefaultJsonProtocol {
  implicit object JodaDateTimeFormat extends RootJsonFormat[DateTime] {
    override def write(d: DateTime) = JsString(d.toString(ISODateTimeFormat.dateTime().withZoneUTC()))

    override def read(v: JsValue) = v match {
      case JsString(s) => DateTime.parse(s, ISODateTimeFormat.dateTime().withZoneUTC())
      case _           => deserializationError("ISO8601 date time expected")
    }
  }
}

object TaskJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  import JodaDateTimeSupport._
  implicit val taskFormat = jsonFormat4(Task)
}

object FriendsRawJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  import JodaDateTimeSupport._
  implicit val friendsRawFormat = jsonFormat5(FriendsRaw)
}

object TaskStatisticsJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  import JodaDateTimeSupport._
  implicit val taskStatisticsFormat = jsonFormat4(TaskStatistics)
}

object FriendsListTaskResultJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  import JodaDateTimeSupport._
  import TaskStatisticsJsonSupport._
  import FriendsRawJsonSupport._
  implicit val frindesListTaskResultFormat = jsonFormat2(FriendsListTaskResult)
}


