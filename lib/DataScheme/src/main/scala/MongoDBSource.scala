package com.vkcrawler.WEBInterface.MongoDB

import com.mongodb.casbah.Imports._
import com.typesafe.config.ConfigFactory
import spray.json._
import java.util.Date
import com.vkcrawler.DataModel._
import com.mongodb.casbah.commons.MongoDBObject
import scala.util.Random

object MongoDBSource {
  import com.mongodb.casbah.commons.conversions.scala._
  import com.typesafe.config.ConfigFactory
  RegisterJodaTimeConversionHelpers()

  val config = ConfigFactory.load()
  val mongoClient = MongoClient(config.getString("MongoDB.host"), config.getInt("MongoDB.port"))
  val db = mongoClient(config.getString("MongoDB.database"))

  def getTask(types: Array[String]): Either[Task, JsObject] = {
    val tasks = db("tasks")

    /* 
      db.tasks.aggregate([
      {$match:{"type": {$in :["raw", "friends_list"]}}},
      {$sort:{lastUseDate:1}},
      {$project:{type:"$type", lud:{date:"$lastUseDate", id:"$_id"}}},
      {$group:{_id:"$type", data:{$first:"$lud"}}},
      {$project:{"_id":"$data.id", type:"$_id", diff:{$subtract: [new Date, "$data.date"]}, date:"$data.date"}}      
    ])
    */
    /*    
    val rawLastTasks = tasks.aggregate(
      List(
        MongoDBObject("$match" ->
          MongoDBObject("type" ->
            MongoDBObject("$in" -> types))),
        MongoDBObject("$sort" ->
          MongoDBObject("lastUseDate" -> 1)),
        MongoDBObject("$project" ->
          MongoDBObject(
            "type" -> "$type",
            "lud" -> MongoDBObject("date" -> "$lastUseDate", "id" -> "$_id"))),
        MongoDBObject("$group" ->
          MongoDBObject(
            "_id" -> "$type", "data" -> MongoDBObject("$first" -> "$lud"))),
        MongoDBObject("$project" ->
          MongoDBObject(
            "_id" -> "$data.id",
            "type" -> "$_id",
            "diff" -> MongoDBObject("$subtract" -> MongoDBList(new Date, "$data.date")),
            "date" -> "$data.date"  
          ))
      ),
      AggregationOptions(allowDiskUse = true)
    )
    

    
    //cache and convert
    val lastTasks = rawLastTasks.toSeq.map { x => 
      (x.as[ObjectId]("_id"), x.as[String]("type"), x.getAs[Long]("diff"), x.getAs[Date]("date"))      
    }
                
    //get frequencies
    //db.tasks_frequency.find({})
    val rawFrequencies = db("tasks_frequency").find(
      MongoDBObject(), 
      MongoDBObject("_id" -> 0)    
    )
    
    val frequencies = rawFrequencies.map { x => 
      (x.as[String]("type"), x.as[Long]("freq"))
    }.toMap

    //filter expired
    val expiredTasks = lastTasks.filter( t =>
      t._3 match {
        case Some(diff) => frequencies(t._2) < diff
        case None => true
      }       
    )

    //choose from random
    val optTask =
    (
        if(expiredTasks.length != 0) //has expired
        {
          val n = Random.nextInt(expiredTasks.length)
          Some(expiredTasks(n));
        }
        else if(lastTasks.length != 0)//has unexpired
        {
          val n = Random.nextInt(lastTasks.length)
          Some(lastTasks(n))
        }
        else
          None
    )
    */

    val candidates = tasks.find(MongoDBObject()).sort(MongoDBObject("lastUseDate" -> 1)).limit(1).map { x => 
      (x.as[ObjectId]("_id"), x.as[String]("type"), x.getAs[Long]("diff"), x.getAs[Date]("date"))      
    }

    if(candidates.hasNext) {
      val task = candidates.next

        tasks.findAndModify(
          MongoDBObject("_id" -> task._1),
          //MongoDBObject("lastUseDate" -> new Date),
          $set("lastUseDate" -> new Date)) 
          match {
            case Some(nTask) => Left(DBConversion.task(nTask))
            case None => Right(
              JsObject("error" ->
                JsObject("description" -> JsString("Modify found task"))))
          }
    }
    else {
        Right(
        JsObject("error" ->
          JsObject("description" -> JsString(s"""No tasks '${types.mkString(", ")}'"""))))
    }

    /*
    optTask match {
      case Some(task) => {
        tasks.findAndModify(
          MongoDBObject("_id" -> task._1),
          //MongoDBObject("lastUseDate" -> new Date),
          $set("lastUseDate" -> new Date)) 
          match {
            case Some(nTask) => Left(DBConversion.task(nTask))
            case None => Right(
              JsObject("error" ->
                JsObject("description" -> JsString("Modify found task"))))
          }
      }
      case None => Right(
        JsObject("error" ->
          JsObject("description" -> JsString(s"""No tasks '${types.mkString(", ")}'"""))))
    }
    */
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