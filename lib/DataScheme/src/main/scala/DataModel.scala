package vkcrawler.DataModel

import com.mongodb.casbah.Imports._
import com.mongodb.DBObject
import org.joda.time.DateTime
import vkcrawler.Common.VKID

import spray.json._

case class TaskData(id:VKID, lastUseDate:Option[DateTime])

case class Task(`type`:String, data: Seq[TaskData])
case class TaskResult(`type`:String, data:JsValue)


// case class FriendsListTask(`type`: String, data: Seq[VKID]) extends Task(`type`)
// case class FriendsListTaskResult(
//   friends: Seq[UserInfo]) extends TaskResult(`type`)
//
// case class WallTask(`type`:String, data: Seq[VKID]) extends Task(`type`)
// case class WallTaskResult(`type`, data:String) extends Task(`type`)

// case class FriendsListTaskResult(
//   friends: Seq[UserInfo])

case class Birthday(day: Int, month: Int, year: Option[Int])

case class UserIdWithCity(uid:VKID, city:Int)

case class UserInfo(
  uid: VKID,
  friends: Seq[UserIdWithCity],
  firstName: String,
  lastName: String,
  birthday: Option[Birthday],
  city: Option[Int],
  interests: Option[String],
  sex: Option[Int],
  processDate: DateTime)


object DBConversion {
  // def task(o: DBObject) = FriendsListTask(
  //   o("type").asInstanceOf[String],
  //   (for (obj <- o("data").asInstanceOf[BasicDBList]) yield obj.asInstanceOf[Number].longValue).toSeq
  // )

  def friendsRaw(o: DBObject) = UserInfo(
    o("id").asInstanceOf[VKID],
    o("friends").asInstanceOf[Seq[UserIdWithCity]],
    o("firstName").asInstanceOf[String],
    o("lastName").asInstanceOf[String],
    o.getAs[Birthday]("birthday"),
    o.getAs[Int]("city"),
    o.getAs[String]("interests"),
    o.getAs[Int]("sex"),
    o("processDate").asInstanceOf[DateTime])
}

object Implicits {
  implicit class UserInfoOps(val fr: UserInfo) {
    def toDB() = fr match {
      case UserInfo(uid, friends, fn, ln, birthday, city, interests, sex, processDate) =>
        MongoDBObject(
          "id" -> uid,
          "friends" -> friends.map{case UserIdWithCity(uid, city) => MongoDBObject("uid" -> uid, "city" -> city)},
          "firstName" -> fn,
          "lastName" -> ln,
          "birthday" -> (birthday match {
                case Some(Birthday(d, m, Some(y))) => MongoDBObject("day"-> d, "month"-> m, "year"-> y)
                case Some(Birthday(d, m, None)) => MongoDBObject("day"-> d, "month"-> m, "year"-> None)
                case None => None
            }),
          "city" -> city,
          "interests" -> interests,
          "sex" -> sex,
          "processDate" -> processDate)
    }
  }
}
