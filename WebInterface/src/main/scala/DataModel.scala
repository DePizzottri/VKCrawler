package com.vkcrawler.DataModel

import com.mongodb.casbah.Imports._
import com.mongodb.DBObject
import org.joda.time.DateTime

case class Task(`type`: String, data: List[Long], createDate: DateTime, lastUseDate: DateTime)

case class TaskStatistics(
  `type`: String,
  createDate: DateTime,
  lastUseDate: DateTime,
  processDate: DateTime)

case class FriendsRawFR(uid:Long, city:Int)

case class FriendsRaw(
  uid: Long,
  friends: List[FriendsRawFR],
  firstName: String,
  lastName: String,
  birthday: DateTime,
  city: Int,
  processDate: DateTime)

case class FriendsListTaskResult(
  task: TaskStatistics,
  friends: List[FriendsRaw])

object DBConversion {
  def task(o: DBObject) = Task(
    o("type").asInstanceOf[String],
    (for (obj <- o("data").asInstanceOf[BasicDBList]) yield obj.asInstanceOf[Number].longValue).toList,
    o("createDate").asInstanceOf[DateTime],
    o.getAsOrElse[DateTime]("lastUseDate", new DateTime))

  def taskStatistics(o: DBObject) = TaskStatistics(
    o("type").asInstanceOf[String],
    o("createDate").asInstanceOf[DateTime],
    o("lastUseDate").asInstanceOf[DateTime],
    o("processDate").asInstanceOf[DateTime])

  def friendsRaw(o: DBObject) = FriendsRaw(
    o("uid").asInstanceOf[Long],
    o("friends").asInstanceOf[List[FriendsRawFR]],
    o("firstName").asInstanceOf[String],
    o("lastName").asInstanceOf[String],
    o("birthday").asInstanceOf[DateTime],
    o("city").asInstanceOf[Int],
    o("processDate").asInstanceOf[DateTime])
}

object Implicits {
  implicit class TaskStatisticsOps(val ts: TaskStatistics) {
    def toDB() = ts match {
      case TaskStatistics(t, createDate, lastUseDate, processDate) =>
        MongoDBObject(
          "type" -> t,
          "createDate" -> createDate,
          "lastUseDate" -> lastUseDate,
          "processDate" -> processDate)
    }
  }

  implicit class FriendsRawOps(val fr: FriendsRaw) {
    def toDB() = fr match {
      case FriendsRaw(uid, friends, fn, ln, birthday, city, processDate) =>
        MongoDBObject(
          "uid" -> uid,
          "friends" -> friends.map{case FriendsRawFR(uid, city) => MongoDBObject("uid" -> uid, "city" -> city)},
          "firstName" -> fn,
          "lastName" -> ln,
          "birthday" -> birthday,
          "city" -> city,
          "processDate" -> processDate)
    }
  }
}