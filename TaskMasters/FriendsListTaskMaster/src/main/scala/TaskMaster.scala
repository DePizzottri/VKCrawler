package com.vkcrawler.taskMasters.FriendsListTaskMaster

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.BulkUpdateRequestBuilder
import scala.util.Random
import com.mongodb.casbah.Imports._
import akka.event.LoggingAdapter
import java.util.Date
import com.mongodb.AggregationOptions.OutputMode

object FriendsListTaskMasterRoutine {
  def getRandomCollection = Random.alphanumeric.take(20).mkString
  
  val mongoClient = MongoClient("localhost", 27017)
  val db = mongoClient("VK_test_2")

  def run(log: LoggingAdapter) {
    import com.mongodb.casbah.commons.conversions.scala._
    //collect all current users
    val current_users = db("friends_list").find(MongoDBObject(), MongoDBObject("uid" -> true, "_id" -> false)).map { obj => obj.as[Long]("uid") }.toList

    if (current_users.length == 0) {
      log.info("No users in friends_list")
    }
    
    val factor = 10000
    
    val task_cnt = db("tasks").find().count()
    
    val tg = 0 :: (1 to (task_cnt / factor) ).map{ x => x * factor}.toList
    
    val task_users = (for(skip <- tg) yield {
      val task_users_agg = db("tasks").aggregate(
        List(
          MongoDBObject("$skip" -> skip),
          MongoDBObject("$limit" -> factor),
          MongoDBObject("$match" -> MongoDBObject("type" -> "friends_list")),
          MongoDBObject("$unwind" -> "$data"),
          MongoDBObject("$group" -> MongoDBObject("_id" -> None, "friends" -> MongoDBObject("$addToSet" -> "$data")))
        ),
        AggregationOptions(allowDiskUse = true)    
      )
      
      task_users_agg.flatMap { obj =>
        obj.get("friends").asInstanceOf[BasicDBList].map { x => x.asInstanceOf[Long] }
      }.toList
    }).flatten.distinct
    
    //diff and find new users
    val new_users = current_users.diff(task_users)

    log.info(s"Finded ${new_users.length} new users")
    
    for (gid <- new_users.zipWithIndex.groupBy { id => id._2 / 50 }) {
      val bld = MongoDBList.newBuilder
    
      gid._2.foreach {
        iid => bld += iid._1
      }
      
      val task = MongoDBObject("type" -> "friends_list", "createDate" -> new Date, "data" -> bld.result())

      db("tasks").insert(task)
    }
  }
}