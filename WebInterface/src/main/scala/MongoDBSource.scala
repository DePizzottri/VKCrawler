package com.vkcrawler.WEBInterface.MongoDB

import com.mongodb.casbah.Imports._
import com.typesafe.config.ConfigFactory
import spray.json._
import java.util.Date

import com.vkcrawler.DataModel._

object MongoDBSource {
  import com.mongodb.casbah.commons.conversions.scala._
  import com.typesafe.config.ConfigFactory
  RegisterJodaTimeConversionHelpers()

  val config = ConfigFactory.load()
  val mongoClient = MongoClient(config.getString("MongoDB.host"), config.getInt("MongoDB.port"))
  val db = mongoClient(config.getString("MongoDB.database"))

  def getTask(types: Array[String]): Either[Task, JsObject] = {
    val tasks = db("tasks")
    val optTask = tasks.findAndModify(
      "type" $in types,
      MongoDBObject("usecount" -> 1),
      $inc("usecount" -> 1) ++ $set("lastUseDate" -> new Date))

    optTask match {
      case Some(task) => Left(DBConversion.task(task))
      case None => Right(
        JsObject("error" ->
          JsObject("description" -> JsString(s"""No tasks '${types.mkString(", ")}'"""))))
    }
  }

  def putTaskResult(result: FriendsListTaskResult) = {
    result match {
      case FriendsListTaskResult(task, res) => {
        import Implicits._
        db("task_statistics").insert(task.toDB())
        val friends_raw = db("friends_raw")
        res.foreach { x => friends_raw.insert(x.toDB()) }
      }
    }
  }
}