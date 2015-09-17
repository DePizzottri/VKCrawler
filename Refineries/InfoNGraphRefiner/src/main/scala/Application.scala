package vkcrawler.refineries.InfoNGraphRefiner

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel
import com.rabbitmq.client.QueueingConsumer
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._
import com.typesafe.config.ConfigFactory
import spray.json._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaLocalDateTimeConversionHelpers
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers

import vkcrawler.DataModel._
import vkcrawler.DataModel.SprayJsonSupport._


object InfoNGraphRefiner extends App {
  RegisterJodaTimeConversionHelpers()
  val conf = ConfigFactory.load()

  val EXCHANGE_NAME = conf.getString("RabbitMQ.exchange")
  val QUEUE_NAME = conf.getString("RabbitMQ.queue_name")
  val ROUTING_KEY = conf.getString("RabbitMQ.routing_key")

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
  val coll = db(conf.getString("MongoDB.collection"))

  //continuously
  // var runTime = 0l
  // var runCnt = 0l
  while(true) {
    //receive message from rabbit
    val delivery = consumer.nextDelivery()
    //val start = System.currentTimeMillis
    val message = new String(delivery.getBody())

    //parse
    import UserInfoJsonSupport._
    val userInfo = message.parseJson.convertTo[UserInfo]

    //put static information
    //and
    //put graph information
    import vkcrawler.DataModel.Implicits._
    coll.insert(userInfo.toDB())

    //confirm delivery
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    // val end = System.currentTimeMillis
    // runTime += end - start
    // runCnt += 1l
    // if(runCnt % 10 == 0 && runCnt != 0) {
    //   println(runTime/runCnt)
    //   runTime = 0l
    //   runCnt = 0l
    // }
  }
}
