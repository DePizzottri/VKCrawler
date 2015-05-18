package com.vkcrawler.taskMasters.FLNGTaskMaster

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel
import com.rabbitmq.client.QueueingConsumer
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._
import com.typesafe.config.ConfigFactory
import spray.json._
import com.vkcrawler.DataModel._
import com.vkcrawler.DataModel.SprayJsonSupport._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaLocalDateTimeConversionHelpers
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import scala.util.Random
import java.util.Date

object FLNGTaskMaster extends App {
  def getRandomCollection = Random.alphanumeric.take(20).mkString

  RegisterJodaTimeConversionHelpers()
  val conf = ConfigFactory.load()

  val EXCHANGE_NAME = "VKCrawler"
  val QUEUE_NAME = "InfoNGraph_TaskMaster"
  val ROUTING_KEY = "info_and_graph"

  //connect to RabbitMQ
  val factory = new ConnectionFactory()
  factory.setHost(conf.getString("RabbitMQ.host"))
  val connection = factory.newConnection()
  val channel = connection.createChannel()

  channel.exchangeDeclare(EXCHANGE_NAME, "direct")
  channel.queueDeclare(QUEUE_NAME, true, false, false, null)
  channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
  channel.basicQos(1);

  //connect to MongoDB
  val mongoClient = MongoClient(conf.getString("MongoDB.host"), conf.getInt("MongoDB.port"))
  val db = mongoClient(conf.getString("MongoDB.database"))
  val uidsCollection = "task_uids_tmp" + getRandomCollection

  db("tasks").aggregate(
    List(
      MongoDBObject("$match" -> MongoDBObject("type" -> "friends_list")),
      MongoDBObject("$unwind" -> "$data"),
      MongoDBObject("$group" -> MongoDBObject("_id" -> None, "uid" -> MongoDBObject("$addToSet" -> "$data"))),
      MongoDBObject("$unwind" -> "$uid"),
      MongoDBObject("$project" -> MongoDBObject("_id" -> "$uid")),
      MongoDBObject("$out" -> uidsCollection)),
    AggregationOptions(allowDiskUse = true))

  //db(uidsCollection).createIndex(MongoDBObject("uid" -> true))

  println("Created temp collection " + uidsCollection)

  val consumer = new QueueingConsumer(channel);
  channel.basicConsume(QUEUE_NAME, false, consumer);

  //continuously
  var cont = List[Long]()
  var runTime = 0l
  var runCnt = 0l
  while (true) {
    //receive message from rabbit
    val delivery = consumer.nextDelivery()
    val start = System.currentTimeMillis
    val message = new String(delivery.getBody())

    //parse
    import FriendsListTaskResultJsonSupport._
    val rawObj = message.parseJson.convertTo[FriendsListTaskResult]

    //put new users to tasks
    import com.vkcrawler.DataModel.Implicits._
    rawObj match {
      case FriendsListTaskResult(task, res) => {
        val allFriends = res.flatMap { x =>
          x match {
            case FriendsRaw(_, friends, _, _, _, _, _, _, _) => friends
            case _ => List[FriendsRawFR]()
          }
        }

        //filter by city here
        val filteredFriends = allFriends.filter { x =>
          if (x.city != 2)
            false
          else {
            val result = db(uidsCollection).update(MongoDBObject("_id" -> x.uid), MongoDBObject(), upsert = true)
            !result.isUpdateOfExisting()
          }
        }.map(_.uid) ++ cont

        val grouped = filteredFriends.zipWithIndex.groupBy { id => id._2 / 50 }
        
        cont = grouped.find(_._2.size != 50) match {
          case Some(elem) => elem._2.map(_._1)
          case None => List[Long]()
        }
        
        for (gid <- grouped; if gid._2.size == 50) {
          val bld = MongoDBList.newBuilder

          gid._2.foreach {
            iid => bld += iid._1
          }

          val task = MongoDBObject("type" -> "friends_list", "createDate" -> new Date, "data" -> bld.result())

          db("tasks").insert(task)
        }
      }
    }// process of new delivery end

    //confirm delivery
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

    val end = System.currentTimeMillis
    runTime += end - start
    runCnt += 1l
    if(runCnt % 10 == 0 && runCnt != 0l) {
      println(runTime/runCnt)
    }
  }
}