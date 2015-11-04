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
      queue.mongodb.database = vkcrawler_test_queue_whole
      graph.mongodb.database = vkcrawler_test_friends_whole
    """.stripMargin
  )
}

class WholeIntegrationSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem(
    "WholeIntegrationSpecSystem",
    WholeIntegrationSpec.config.withFallback(
      MongoRichQueueSpec.config.withFallback(
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

  def cleanRedis = {
    //clean redis
    val conf = system.settings.config
    val jedis = new Jedis(conf.getString("used.redis.host"), conf.getInt("used.redis.port"))
    val uidsSet = conf.getString("used.redis.setName")
    jedis.del(uidsSet)
  }

  override def beforeAll {
    //used
    cleanRedis
  }

  override def afterAll {
    //cleanup
    system.shutdown()

    val conf = system.settings.config
    val qmongoClient = MongoClient(conf.getString("queue.mongodb.host"), conf.getInt("queue.mongodb.port"))

    //queue
    val qdb = qmongoClient(conf.getString("queue.mongodb.database"))
    qdb.dropDatabase

    //graph
    val gmongoClient = MongoClient(conf.getString("graph.mongodb.host"), conf.getInt("graph.mongodb.port"))
    val gdb = gmongoClient(conf.getString("graph.mongodb.database"))
    gdb.dropDatabase

    //used
    cleanRedis

    //rabbit?
  }

  "All integrated BFS actors " must {
    "process simple graph " in {
      import vkcrawler.Common._

      //creating
      //class QueueMongoDBActor extends ReliableQueueActor with ReliableMongoQueueBackend
      class RichQueueMongoDBActor extends ReliableRichQueueActor {
        class MongoBackendActor extends RichQueueBackendActor with MongoRichQueueBackend
        override val persistenceId = "queue-whole-int"
        override def createBackend = new MongoBackendActor
        override val demandThreshold = 2
      }

      val queue = system.actorOf(Props(new RichQueueMongoDBActor), "QueueActor")

      val bfspath = ActorPath.fromString(system.toString+"/user/BFSActor")

      val friendsConsumer = new ExchangeRabbitMQSpec.Consumer(system, "friends")
      val newUsersConsumer = new ExchangeRabbitMQSpec.Consumer(system, "new_users")

      class RabbitMQExchangeActor extends ReliableExchangeActor(
        bfspath,
        queue.path
      ) with RabbitMQExchangeBackend {
        override val persistenceId = "exchange-whole-int"
      }
      val exchange = system.actorOf(Props(new RabbitMQExchangeActor), "ExchangeActor")

      class GraphSaverMongoDBActor extends ReliableGraphActor with ReliableMongoDBGraphSaverBackend
      val graph = system.actorOf(Props(new GraphSaverMongoDBActor), "GraphActor")

      class JedisUsedActor extends ReliableUsedActor with JedisUsedBackend {
        override val persistenceId = "used-whole-int"
      }
      val used = system.actorOf(Props(new JedisUsedActor), "UsedActor")

      class WholeBFSActor extends ReliableBFSActor(graph.path, used.path, exchange.path) {
        override val persistenceId = "bfs-whole-int"
      }
      val bfs = system.actorOf(Props(new WholeBFSActor), "BFSActor")

      import ReliableMessaging._

      //init
      queue ! Envelop(1, RichQueue.Push(Seq(1l)))
      expectMsg(Confirm(1))

      used ! Used.InsertAndFilter(Seq(1l))
      expectMsg(Envelop(1, Used.Filtered(Seq(1))))
      used ! Confirm(1)

      val g = Map[VKID, Seq[VKID]](1l -> Seq[VKID](2l, 3l), 2l->Seq[VKID](4l), 4l->Seq(5l))

      //start
      class Crawler extends Actor with ReliableMessaging {
        import vkcrawler.Common._

        import scala.concurrent.ExecutionContext.Implicits.global
        system.scheduler.schedule(100.milliseconds, 100.milliseconds, self, "Run")

        override def persistenceId: String = "crawler-actor-id"

        override def processCommand: Receive = {
          case "Run" => {
            deliver(RichQueue.Pop(Seq("task1")), queue.path)
          }
          case RichQueue.Item(task) => {
            //println(task)
            task.data.foreach{ taskData =>
              deliver(BFS.Friends(taskData.id, g.getOrElse(taskData.id, Seq())), exchange.path)
            }
          }
        }
      }

      val crawler = system.actorOf(Props(new Crawler), "CrawlerActor")

      expectNoMsg(10.seconds)

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
      import scala.collection.JavaConverters._
      if (jedis.scard(uidsSet) != 5)
      {
        println(jedis.smembers(uidsSet).asScala)
      }
      jedis.scard(uidsSet) should be (5)

      //check emitted from excahnge

      friendsConsumer.consumeOne
      //friendsConsumer.consumeOne
      newUsersConsumer.consumeOne
      //newUsersConsumer.consumeOne

      //friendsConsumer.published should be (Seq("Friends(1,List(2, 3))", "Friends(2,List(4))"))
      friendsConsumer.published should be (Seq("Friends(1,List(2, 3))"))
      newUsersConsumer.published should be (Seq("NewUsers(List(2, 3))"))
    }
  }
}
