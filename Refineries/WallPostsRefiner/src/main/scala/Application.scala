package vkcrawler.refineries.WallPostsRefiner

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

import com.mongodb.util.JSON

import org.joda.time.DateTime

object WallPostsRefiner extends App {
  RegisterJodaTimeConversionHelpers()

  // import com.mongodb.util.JSON;
  // val conf = ConfigFactory.load()
  // val mongoClient = MongoClient(conf.getString("MongoDB.host"), conf.getInt("MongoDB.port"))
  // val db = mongoClient(conf.getString("MongoDB.database"))
  // val coll = db(conf.getString("MongoDB.collection"))
  //
  // val j = """{
  //   "a": 10
  // }"""
  //
  // val obj = JSON.parse(j)
  //
  // coll.insert(obj.asInstanceOf[BasicDBObject])
  //
  // println(obj)
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

  channel.exchangeDeclare(EXCHANGE_NAME, "direct", true)
  channel.queueDeclare(QUEUE_NAME, true, false, false, null)
  channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
  channel.basicQos(100);

  val consumer = new QueueingConsumer(channel);
  channel.basicConsume(QUEUE_NAME, false, consumer);

  //connect to MongoDB
  val mongoClient = MongoClient(conf.getString("MongoDB.host"), conf.getInt("MongoDB.port"))
  val db = mongoClient(conf.getString("MongoDB.database"))
  val coll = db(conf.getString("MongoDB.collection"))

  //continuously
  while(true) {
    //receive message from rabbit

    val delivery = consumer.nextDelivery()
    val message = new String(delivery.getBody())

    //parse
    val obj = JSON.parse(message).asInstanceOf[BasicDBObject]

    val dateTime = obj.getAs[Int]("date") match {
      case Some(dt) => {
        val ldt: Long = dt
        obj += "date" -> new DateTime(ldt * 1000L)
      }
      case None => println("No date field")
    }

    coll.update(
      q = MongoDBObject("owner_id" -> obj.as[Int]("owner_id"), "date" -> obj.as[DateTime]("date")),
      o = obj,
      upsert = true,
      multi = false
    )

    //confirm delivery
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
  }
}
