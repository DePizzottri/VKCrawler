package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe
import scala.util.Random
import com.typesafe.config.ConfigFactory

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel
import com.rabbitmq.client.QueueingConsumer

import spray.json._

object ExchangeRabbitMQSpec {
  private def getRandomCollection = Random.alphanumeric.take(5).mkString

  def config = ConfigFactory.parseString(
    """
    exchange {
      rabbitmq {
        host = localhost
        username = guest
        password = guest
        exchange_name = VKCrawler
      }
    }
    """.stripMargin
  )

  class Consumer(system:ActorSystem, routingKey:String) {
    val factory = new ConnectionFactory()
    val config = system.settings.config
    factory.setHost(config.getString("exchange.rabbitmq.host"))
    //factory.setPort(config.getInt("exchange.rabbitmq.port"))
    factory.setUsername(config.getString("exchange.rabbitmq.username"))
    factory.setPassword(config.getString("exchange.rabbitmq.password"))

    val EXCHANGE_NAME = config.getString("exchange.rabbitmq.exchange_name")

    val connection = factory.newConnection()
    val channel = connection.createChannel()

    channel.exchangeDeclare(EXCHANGE_NAME, "direct", true)
    val QUEUE_NAME = channel.queueDeclare().getQueue()
    channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, routingKey);
    channel.basicQos(1);

    val consumer = new QueueingConsumer(channel);
    channel.basicConsume(QUEUE_NAME, false, consumer);

    var published = scala.collection.mutable.MutableList.empty[String]
    def consumeOne {
      val delivery = consumer.nextDelivery()
      val message = new String(delivery.getBody())
      published += message
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    }

    def finish {

    }
  }
}

class ExchangeRabbitMQSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem(
    "ExchangeRabbitMQSpecSystem",
    ExchangeRabbitMQSpec.config.withFallback(PersistanceSpecConfiguration.config)))

  override def beforeAll = {
  }

  override def afterAll = {
    system.shutdown()
  }

  import vkcrawler.bfs._
  import vkcrawler.bfs.SprayJsonSupport._

  "RabbitMQExchangeActor " must {
    "accept with confirm, both send to BFS and Queue and publish" in {
      val bfs = TestProbe()
      val queue = TestProbe()

      class RabbitMQExchangeActor extends ReliableExchangeActor(bfs.ref.path, queue.ref.path) with RabbitMQExchangeBackend
      val exchange = system.actorOf(Props(new RabbitMQExchangeActor))

      val friendsConsumer = new ExchangeRabbitMQSpec.Consumer(system, "friends")
      val newUsersConsumer = new ExchangeRabbitMQSpec.Consumer(system, "new_users")

      import vkcrawler.Common._
      val friends = BFS.Friends(1, Seq[VKID](2,3,4))
      exchange ! friends

      import ReliableMessaging._
      val ebsf = bfs.expectMsgClass(classOf[Envelop])
      ebsf.msg should be (friends)
      exchange ! Confirm(ebsf.deliveryId)

      val newUsers = BFS.NewUsers(Seq[VKID](2,3,4))
      exchange ! newUsers
      val eq = queue.expectMsgClass(classOf[Envelop])
      eq.msg should be (RichQueue.Push(newUsers.users))
      exchange ! Confirm(eq.deliveryId)

      expectNoMsg(500.milliseconds)

      friendsConsumer.consumeOne
      newUsersConsumer.consumeOne

      import FriendsJsonSupport._
      import NewUsersJsonSupport._

      friendsConsumer.published.map{_.parseJson} should be (List(friends.toJson))
      newUsersConsumer.published.map{_.parseJson} should be (List(newUsers.toJson))
    }
  }
}
