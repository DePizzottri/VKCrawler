package com.vkcrawler.refineries.FriendsListRefiner

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.BulkUpdateRequestBuilder
import scala.util.Random
import com.mongodb.casbah.Imports._
import akka.event.LoggingAdapter

object FriendsListRefinerRoutine {
  def getRandomCollection = Random.alphanumeric.take(20).mkString

  val mongoClient = MongoClient("192.168.1.9", 27017)
  val db = mongoClient("VK_Piter")

  def run(log: LoggingAdapter) {
    val friends_raw_tmp = getRandomCollection
    //println("Work on " + friends_raw_tmp)
    import com.mongodb.casbah.commons.conversions.scala._
    //find new users
    //collect all current users
    //val currentUsers = db("friends_list").distinct("uid")
    //val currentUsers = db("friends_list").find(MongoDBObject(), MongoDBObject("uid" -> true, "_id" -> false)).map { obj => obj.as[Long]("uid") }.toSeq
    /*
    
    val curUsersCount = db("friends_list").count()
    
    //if zero start from First man
    if (curUsersCount == 0) {
      db("first_man").findOne() match {
        case Some(fm) => {
          db("friends_list").insert(MongoDBObject("uid" -> fm.get("uid").asInstanceOf[Long]))
          log.info("First man added")
        }
        case None => throw new Exception("No First man finded!")
      }
    }

    */
    log.info("Moving to " + friends_raw_tmp)
    
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
    val factor = 1000
    val cnt = db(friends_raw_tmp).find().count()
    val batches = 0 :: (1 to (cnt / factor) ).map{ x => x * factor}.toList

    log.info("Aggregate")
    val raw_users = (for(skip <- batches) yield {
      val raw_users_c = db(friends_raw_tmp).aggregate(
        List(
          MongoDBObject("$skip" -> skip),
          MongoDBObject("$limit" -> factor),
          MongoDBObject("$unwind" -> "$friends"),
          MongoDBObject("$match" -> MongoDBObject("friends.city" -> 2)),
          MongoDBObject("$group" -> MongoDBObject("_id" -> None, "friends" -> MongoDBObject("$addToSet" -> "$friends.uid")))))

      raw_users_c.results.flatMap { obj =>
        obj.get("friends").asInstanceOf[BasicDBList].map { x => x.asInstanceOf[Long] }
      }
    }).flatten.toSet
    
    log.info("Intersect and diff")
    val intersection = db("friends_list").find(MongoDBObject(), MongoDBObject("uid" -> true, "_id" -> false)).flatMap { obj =>
      val uid = obj.as[Long]("uid")
      if(raw_users.contains(uid))
        Seq(uid)
      else
        Seq()
    }.toSeq    

    //diff and find new users
    val new_users = raw_users.toSeq.diff(intersection)

    log.info(s"Finded ${new_users.length} new users")

    //insert new users
    val newUsersInsert = db("friends_list").initializeUnorderedBulkOperation
    new_users.foreach { x =>
      //db("friends_list").insert(MongoDBObject("uid" -> x))
      newUsersInsert.insert(MongoDBObject("uid" -> x))
    }

    try{
      newUsersInsert.execute();
    }
    catch {
      case e:IllegalStateException => None
      case t: Throwable => t.printStackTrace() // TODO: handle error
    }
    log.info("New users inserted")
    

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
    log.info("Copy finished")
    
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
    log.info("BULK executed")

    db(friends_raw_tmp).drop()
    System.gc(); 
  }
}