package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random

object QueueMongoDBIntegration {
  private def getRandomCollection = Random.alphanumeric.take(5).mkString

  val popSize = 5

  def config = ConfigFactory.parseString(
    s"""
    queue {
      mongodb {
        host = localhost
        port = 27017
        database = vkcrawler_queue_test
        queue = queue
      }

      popSize = $popSize
    }
    """.stripMargin
  )
}

class QueueMongoDBIntegration(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(
    ActorSystem(
      "QueueMongoDBIntegrationSpecSystem",
      QueueMongoDBIntegration.config.withFallback(PersistanceSpecConfiguration.config)
      )
    )

  override def afterAll {
    //cleanup
    import com.mongodb.casbah.MongoClient
    import com.mongodb.casbah.Imports._

    val conf = system.settings.config
    var mongoClient = MongoClient(conf.getString("queue.mongodb.host"), conf.getInt("queue.mongodb.port"))
    var db = mongoClient(conf.getString("queue.mongodb.database"))

    db.dropDatabase
    system.terminate()
  }

  import vkcrawler.bfs._
  import vkcrawler.DataModel._

  class QueueMongoDBActor extends ReliableQueueActor with ReliableMongoQueueBackend

  "QueueMongoDBActor " must {
    "return empty on emtpy queue" in {
      import ReliableMessaging._
      val queue = system.actorOf(Props(new QueueMongoDBActor))
      queue ! Envelop(13, Queue.Pop("stub"))

      expectMsg(Confirm(13))
      val e = expectMsgClass(classOf[Envelop])
      e.msg should be (Queue.Item(Task("stub", Seq())))
      queue ! Confirm(e.deliveryId)
      expectNoMsg(1.seconds)
    }

    "preserve queue order" in {
      import vkcrawler.Common._
      import ReliableMessaging._

      val popSize = QueueMongoDBIntegration.popSize
      val queue = system.actorOf(Props(new QueueMongoDBActor))
      val ins = (1l to popSize * 2).toSeq
      queue ! Queue.Push(ins)
      queue ! Queue.Pop("stub")
      //expectMsg(10.seconds, Envelop(2, Queue.Items(1l to popSize)))
      expectMsg(
        10.seconds,
        Envelop(
          2,
          Queue.Item(
            Task("stub", (1l to popSize).map{id => TaskData(id, None)})
          )
        )
      )
      queue ! Queue.Pop("stub")
      expectMsg(
        10.seconds,
        Envelop(
          3,
          Queue.Item(
            Task("stub", (1l to popSize).map{id => TaskData(id + popSize, None)})
          )
        )
      )
    }

    "be idempotent " in {
      import vkcrawler.Common._
      import ReliableMessaging._
      val popSize = QueueMongoDBIntegration.popSize

      val queue = system.actorOf(Props(new QueueMongoDBActor{
        override def persistenceId = Random.alphanumeric.take(5).mkString
      }))

      val ins = Seq[VKID](1, 1, 1)
      queue ! Queue.Push(ins)
      queue ! Queue.Pop("stub2")
      //expectMsg(10.seconds, Envelop(2, Queue.Items(1l to popSize)))
      expectMsg(
        10.seconds,
        Envelop(
          1,
          Queue.Item(
            Task("stub2", (1l to popSize).map{id => TaskData(id, None)})
          )
        )
      )
      queue ! Queue.Pop("stub2")
      expectMsg(
        10.seconds,
        Envelop(
          2,
          Queue.Item(
            Task("stub2", (1l to popSize).map{id => TaskData(id + popSize, None)})
          )
        )
      )
    }
  }
}
