package com.vkcrawler.refineries.FriendsListRefiner

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.BulkUpdateRequestBuilder
import scala.util.Random
import com.mongodb.casbah.Imports._
import akka.event.LoggingAdapter

object FriendsListRefinerRoutine {
  def getRandomCollection = Random.alphanumeric.take(20).mkString

  val mongoClient = MongoClient("localhost", 27017)
  val db = mongoClient("VK_test_2")
  val friends_raw_tmp = getRandomCollection

  def run(log: LoggingAdapter) {
    //println("Work on " + friends_raw_tmp)
    import com.mongodb.casbah.commons.conversions.scala._
    //find new users
    //collect all current users
    val currentUsers = db("friends_list").distinct("uid")

    //if zero start from First man
    if (currentUsers.length == 0) {
      db("first_man").findOne() match {
        case Some(fm) => {
          db("friends_list").insert(MongoDBObject("uid" -> fm.get("uid").asInstanceOf[Long]))
          log.info("First man added")
        }
        case None => throw new Exception("No First man finded!")
      }
    }

    //move working set
    {
      val bulkInsert = db(friends_raw_tmp).initializeUnorderedBulkOperation
      val bulkRemove = db("friends_raw").initializeUnorderedBulkOperation

      db("friends_raw").find().limit(5000).foreach { obj =>
        val uid = obj.get("uid").asInstanceOf[Long]
        //db(friends_raw_tmp).insert(obj)
        bulkInsert.insert(obj)
        //db("friends_raw").remove(MongoDBObject("uid" -> uid))
        bulkRemove.find(MongoDBObject("uid" -> uid)).removeOne()
      }

      try {
        bulkInsert.execute()
      }
      catch {
        case e:IllegalStateException => None
        case t: Throwable => t.printStackTrace()
      }

      try {
        bulkRemove.execute()
      }
      catch {
        case e:IllegalStateException => None
        case t: Throwable => t.printStackTrace()
      }
    }

    //collect raw users
    val raw_users_c = db(friends_raw_tmp).aggregate(
      List(
        MongoDBObject("$unwind" -> "$friends"),
        MongoDBObject("$match" -> MongoDBObject("friends.city" -> 148)),
        MongoDBObject("$group" -> MongoDBObject("_id" -> None, "friends" -> MongoDBObject("$addToSet" -> "$friends.uid")))))

    val raw_users = raw_users_c.results.toList.flatMap { obj =>
      obj.get("friends").asInstanceOf[BasicDBList].map { x => x.asInstanceOf[Long] }
    }

    //intersect and find new users
    val new_users = raw_users.diff(currentUsers)

    log.info(s"Finded ${new_users.length} new users")

    //insert new users
    new_users.foreach { x =>
      db("friends_list").insert(MongoDBObject("uid" -> x))
    }

    val bulkInsertDyn = db("friends_dynamic").initializeUnorderedBulkOperation
    val bulkUpdateList = db("friends_list").initializeUnorderedBulkOperation

    //update info and insert dynamics
    db(friends_raw_tmp).find().foreach { rawObj =>
      val old_friends_opt = db("friends_list")
        .findOne(MongoDBObject("uid" -> rawObj.get("uid").asInstanceOf[Long]))

      //newly collected friends list
      val new_friends = rawObj.getAsOrElse[BasicDBList]("friends", new BasicDBList())
        .map { x =>
          x.asInstanceOf[DBObject].get("uid").asInstanceOf[Long]
        }
      if (old_friends_opt.exists { _.containsField("friends") }) {
        val old_friends = old_friends_opt.get.get("friends").asInstanceOf[BasicDBList].map { x => x.asInstanceOf[Long] }

        //insert dynamics information
        bulkInsertDyn.insert(MongoDBObject(
          "uid" -> rawObj.get("uid"),
          "friendsAdded" -> new_friends.diff(old_friends),
          "friendsDeleted" -> old_friends.diff(new_friends),
          "birthday" -> rawObj.get("birthday"),
          "processDate" -> rawObj.get("processDate")))
      }

      //update all data
      bulkUpdateList.find(MongoDBObject("uid" -> rawObj.get("uid")))
        .update(
          $set(
            "friends" -> new_friends,
            "firstName" -> rawObj.get("firstName"),
            "lastName" -> rawObj.get("lastName"),
            "birthday" -> rawObj.get("birthday"),
            "city" -> rawObj.get("city"),
            "interests" -> rawObj.get("interests"),
            "sex" -> rawObj.get("sex")))

    }
    
    try
    {
      bulkInsertDyn.execute();
    }
    catch {
      case e:IllegalStateException => None
      case t: Throwable => t.printStackTrace() // TODO: handle error
    }
    
    try {
      bulkUpdateList.execute();
    }
    catch {
      case e:IllegalStateException => None
      case t: Throwable => t.printStackTrace() // TODO: handle error
    }

    db(friends_raw_tmp).drop()
  }
}