package com.vkcrawler.refineries.InfoNGraphRefiner

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

object InfoNGraphRefiner extends App {
  RegisterJodaTimeConversionHelpers()
  val conf = ConfigFactory.load()
  
  val EXCHANGE_NAME = "VKCrawler"
  val QUEUE_NAME = "InfoNGraph_Refiner"
  val ROUTING_KEY = "info_and_graph"
  
  //connect to RabbitMQ
  val factory = new ConnectionFactory()
  factory.setHost(conf.getString("RabbitMQ.host"))
  factory.setUsername(conf.getString("RabbitMQ.username"))
  factory.setPassword(conf.getString("RabbitMQ.password"))
  val connection = factory.newConnection()
  val channel = connection.createChannel()
  
  channel.exchangeDeclare(EXCHANGE_NAME, "direct")
  channel.queueDeclare(QUEUE_NAME, true, false, false, null)
  channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY); 
  channel.basicQos(1);
  
  val consumer = new QueueingConsumer(channel);
  channel.basicConsume(QUEUE_NAME, false, consumer);
  
  //connect to MongoDB
  val mongoClient = MongoClient(conf.getString("MongoDB.host"), conf.getInt("MongoDB.port"))
  val db = mongoClient(conf.getString("MongoDB.database"))
  
  //continuously
  var runTime = 0l
  var runCnt = 0l
  while(true) {
    //receive message from rabbit
    val delivery = consumer.nextDelivery()
    val start = System.currentTimeMillis
    val message = new String(delivery.getBody())
    
    //parse
    import FriendsListTaskResultJsonSupport._
    val rawObj = message.parseJson.convertTo[FriendsListTaskResult]

    //put static information
    //and
    //put graph information
    import com.vkcrawler.DataModel.Implicits._
    rawObj match {
      case FriendsListTaskResult(task, res) => {
        val user_info_and_graph = db("user_info_and_graph")
        res.foreach { x => user_info_and_graph.insert(x.toDB()) }
      }
    }
    
    //confirm delivery
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    val end = System.currentTimeMillis
    runTime += end - start
    runCnt += 1l
    if(runCnt % 10 == 0 && runCnt != 0) {
      println(runTime/runCnt)
      runTime = 0l
      runCnt = 0l
    }
  }
}