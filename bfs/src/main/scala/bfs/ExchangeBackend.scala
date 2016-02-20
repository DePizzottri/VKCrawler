package vkcrawler.bfs

import spray.json.JsValue

trait ExchangeBackend {
  def init:Unit
  def publish(tag:String, msg: JsValue):Unit
}

trait DummyExchangeBackend extends ExchangeBackend {
  def init:Unit = {}
  def publish(tag:String, msg: JsValue):Unit = {}
}

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.AMQP.BasicProperties

import com.typesafe.config.ConfigFactory

trait RabbitMQExchangeBackend extends ExchangeBackend {
  this: akka.actor.Actor =>
  val conf = context.system.settings.config

  val factory = new ConnectionFactory()
  factory.setHost(conf.getString("exchange.rabbitmq.host"))
  //factory.setPort(conf.getInt("exchange.rabbitmq.port"))
  factory.setUsername(conf.getString("exchange.rabbitmq.username"))
  factory.setPassword(conf.getString("exchange.rabbitmq.password"))

  val EXCHANGE_NAME = conf.getString("exchange.rabbitmq.exchange_name")
  var channel:Channel = null

  def init:Unit = {
    //connect to RabbitMQ
    val connection = factory.newConnection()
    channel = connection.createChannel()

    channel.exchangeDeclare(EXCHANGE_NAME, "direct", true)
  }

  def publish(tag:String, msg: JsValue):Unit = {
    channel.basicPublish(EXCHANGE_NAME, tag, MessageProperties.PERSISTENT_TEXT_PLAIN, msg.toString.getBytes());
  }
}
