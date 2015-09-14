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

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._

import redis.clients.jedis._


object WholeIntegrationSpec {
  private def getRandomCollection = Random.alphanumeric.take(5).mkString

  def config = ConfigFactory.parseString(
    """
    """.stripMargin
  )
}

class WholeIntegrationSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem(
    "WholeIntegrationSpecSystem",
    WholeIntegrationSpec.config.withFallback(
      QueueMongoDBIntegration.config.withFallback(
        ExchangeRabbitMQSpec.config.withFallback(
          GraphSaverMongoDBIntegrationSpec.config.withFallback(
            JedisUsedSpec.config.withFallback(
              PersistanceSpecConfiguration.config
            )
          )
        )
      )
    )
  ))

  import vkcrawler.bfs._

  override def afterAll {
    //cleanup
    val conf = system.settings.config
    val mongoClient = MongoClient(conf.getString("queue.mongodb.host"), conf.getInt("queue.mongodb.port"))

    //queue
    val qdb = mongoClient(conf.getString("queue.mongodb.database"))
    qdb.dropDatabase

    //graph
    val gdb = mongoClient(conf.getString("graph.mongodb.database"))
    gdb.dropDatabase

    //used
    val jedis = new Jedis(conf.getString("used.redis.host"), conf.getInt("used.redis.port"))
    val uidsSet = conf.getString("used.redis.setName")
    jedis.del(uidsSet)

    //rabbit?

    system.shutdown()
  }

  "All integrated BFS actors " must {
    "process simple graph " in {
      import vkcrawler.Common._

      //creating
      class QueueMongoDBActor extends ReliableQueueActor with ReliableMongoQueueBackend
      val queue = system.actorOf(Props(new QueueMongoDBActor), "QueueActor")

      val bfspath = ActorPath.fromString(system.toString+"/user/BFSActor")

      val friendsConsumer = new ExchangeRabbitMQSpec.Consumer(system, "friends")
      val newUsersConsumer = new ExchangeRabbitMQSpec.Consumer(system, "new_users")

      class RabbitMQExchangeActor extends ReliableExchangeActor(
        bfspath,
        queue.path
      ) with RabbitMQExchangeBackend
      val exchange = system.actorOf(Props(new RabbitMQExchangeActor), "ExchangeActor")

      class GraphSaverMongoDBActor extends ReliableGraphActor with ReliableMongoDBGraphSaverBackend
      val graph = system.actorOf(Props(new GraphSaverMongoDBActor), "GraphActor")

      class JedisUsedActor extends ReliableUsedActor with JedisUsedBackend
      val used = system.actorOf(Props(new JedisUsedActor), "UsedActor")

      val bfs = system.actorOf(Props(new ReliableBFSActor(graph.path, used.path, exchange.path)), "BFSActor")

      import ReliableMessaging._

      //init
      queue ! Envelop(1, Queue.Push(Seq(1l)))
      expectMsg(Confirm(1))

      used ! Used.InsertAndFilter(Seq(1l))
      expectMsg(Envelop(1, Used.Filtered(Seq(1))))
      used ! Confirm(1)

      val g = Map[VKID, Seq[VKID]](1l -> Seq[VKID](2l, 3l), 2l->Seq[VKID](4l), 4l->Seq(5l))

      //start
      class Crawler extends Actor with ReliableMessaging {
        import vkcrawler.Common._

        import scala.concurrent.ExecutionContext.Implicits.global
        system.scheduler.schedule(20.milliseconds, 20.milliseconds, self, "Run")

        override def persistenceId: String = "crawler-actor-id"

        override def processCommand: Receive = {
          case "Run" => {
            deliver(Queue.Pop, queue.path)
          }
          case Queue.Empty => {
          }
          case Queue.Items(items) => {
            items.foreach{ id =>
              deliver(BFS.Friends(id, g.getOrElse(id, Seq())), exchange.path)
            }
          }
        }
      }

      val crawler = system.actorOf(Props(new Crawler), "CrawlerActor")

      expectNoMsg(2.seconds)

      val conf = system.settings.config
      //check saved graph
      val gmongoClient = MongoClient(conf.getString("graph.mongodb.host"), conf.getInt("graph.mongodb.port"))
      //graph
      val gdb = gmongoClient(conf.getString("graph.mongodb.database"))
      val gcol = gdb(conf.getString("graph.mongodb.friends"))
      gcol.find().length should be (5)

      //check queue
      val qmongoClient = MongoClient(conf.getString("queue.mongodb.host"), conf.getInt("queue.mongodb.port"))
      val qdb = qmongoClient(conf.getString("queue.mongodb.database"))
      val qcol = qdb(conf.getString("queue.mongodb.queue"))

      qcol.find().length should be (5)

      //check used
      val jedis = new Jedis(conf.getString("used.redis.host"), conf.getInt("used.redis.port"))
      val uidsSet = conf.getString("used.redis.setName")
      jedis.scard(uidsSet) should be (5)

      //check emitted from excahnge

      friendsConsumer.consumeOne
      friendsConsumer.consumeOne
      newUsersConsumer.consumeOne
      newUsersConsumer.consumeOne

      friendsConsumer.published should be (Seq("Friends(1,List(2, 3))", "Friends(2,List(4))"))
      newUsersConsumer.published should be (Seq("NewUsers(List(2, 3))", "NewUsers(List(4))"))
    }
  }
}
