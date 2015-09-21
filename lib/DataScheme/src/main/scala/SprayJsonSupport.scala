package vkcrawler.DataModel.SprayJsonSupport

import spray.json._
import spray.json.DefaultJsonProtocol
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import spray.httpx.SprayJsonSupport
import vkcrawler.DataModel._
import java.util.Date
import org.joda.time.DateTimeZone

object JodaDateTimeSupport extends DefaultJsonProtocol {
  implicit object JodaDateTimeFormat extends RootJsonFormat[DateTime] {
    override def write(d: DateTime) = JsString(d.toString(ISODateTimeFormat.dateTime().withZoneUTC()))

    override def read(v: JsValue) = v match {
      case JsString(s) => DateTime.parse(s, ISODateTimeFormat.dateTime().withZone(DateTimeZone.getDefault))
      case _           => deserializationError("ISO8601 date time expected")
    }
  }
}

object TaskJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val taskFormat = jsonFormat2(Task)
}


object TaskResultJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val taskResulFormat = jsonFormat2(TaskResult)
}

// object FriendsListTaskJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
//   import JodaDateTimeSupport._
//   implicit val taskFormat = jsonFormat2(FriendsListTask)
// }

object UserIdWithCityJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val friendsRawFRFormat = jsonFormat2(UserIdWithCity)
}

object BirthdayJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val birthdayFormat = jsonFormat3(Birthday)
}

object UserInfoJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  import JodaDateTimeSupport._
  import BirthdayJsonSupport._
  import UserIdWithCityJsonSupport._
  implicit val friendsRawFormat = jsonFormat9(UserInfo)
}

// object FriendsListTaskResultJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
//   import JodaDateTimeSupport._
//   import UserInfoJsonSupport._
//   implicit val frindesListTaskResultFormat = jsonFormat1(FriendsListTaskResult)
// }
