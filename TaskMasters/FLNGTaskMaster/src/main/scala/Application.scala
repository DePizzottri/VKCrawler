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

import com.redis._

object FLNGTaskMaster extends App {
  def getRandomCollection = Random.alphanumeric.take(5).mkString

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
  val uidsSet = "temp_user_ids" //+ getRandomCollection

  //connect to redis
  val redis = new RedisClient(conf.getString("Redis.host"), conf.getInt("Redis.port"))

  if(args.length > 0 && args(0) == "secondary") {
    println("Use created temp set " + uidsSet)
  }
  else {
    val s = System.currentTimeMillis
    db("tasks").find(MongoDBObject("type" -> "friends_list"), MongoDBObject("_id" -> 0, "data" -> 1)).foreach( obj => 
      obj.as[MongoDBList]("data").foreach( uid =>
        redis.sadd(uidsSet, uid.asInstanceOf[Long])
      )
    )
    val e = System.currentTimeMillis
    println("Created temp set " + uidsSet + s" in ${e-s}ms")
  }

  val consumer = new QueueingConsumer(channel);
  channel.basicConsume(QUEUE_NAME, false, consumer);

  //continuously
  var runTime = 0l
  var runCnt = 0l
  var filterTime = 0l
  var insertTime = 0l
  var parseTime = 0l
  while (true) {
    //receive message from rabbit
    val delivery = consumer.nextDelivery()
    val start = System.currentTimeMillis
    val pstart = System.currentTimeMillis
    val message = new String(delivery.getBody())

    //parse
    import FriendsListTaskResultJsonSupport._
    val rawObj = message.parseJson.convertTo[FriendsListTaskResult]
    parseTime += System.currentTimeMillis - pstart

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
        val fstart = System.currentTimeMillis
        val filteredFriends = allFriends.filter { x =>
          if (x.city != 2)
            false
          else {
            redis.sadd(uidsSet, x.uid) match {
              case Some(res) => res == 1l
              case None => {println("None answer"); false}
            }
          }
        }.map(_.uid)
        val fend = System.currentTimeMillis
        filterTime += fend-fstart

        val grouped = filteredFriends.zipWithIndex.groupBy { id => id._2 / 50 }

        val istart = System.currentTimeMillis
        val bulkInsert = db("tasks").initializeUnorderedBulkOperation
        for (gid <- grouped) {
          val bld = MongoDBList.newBuilder

          gid._2.foreach {
            iid => bld += iid._1
          }

          val task = MongoDBObject("type" -> "friends_list", "createDate" -> new Date, "data" -> bld.result())

          bulkInsert.insert(task)
        }
        bulkInsert.execute()
        val iend = System.currentTimeMillis
        insertTime += iend-istart
      }
    }// process of new delivery end

    //confirm delivery
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

    val end = System.currentTimeMillis
    runTime += end - start
    runCnt += 1l
    if(runCnt % 10 == 0 && runCnt != 0l) {
      println(s"Parse time ${parseTime/runCnt}")
      println(s"Filter time ${filterTime/runCnt}")
      println(s"Insert time ${insertTime/runCnt}")
      println(s"Total time ${runTime/runCnt}")
      println()
      runTime = 0l
      runCnt = 0l
      filterTime = 0l
      insertTime = 0l
      parseTime = 0l
    }
  }
}