package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random


import akka.actor._
import scala.concurrent.duration._

object MongoRichQueueSpec {
  val taskSize = 5
  val batchSize = 10

  def config = ConfigFactory.parseString(
    s"""
    queue {
      mongodb {
        host = 127.0.0.1
        port = 27017
        database = vkcrawler_queue_test
        queue = queue
      }

      taskSize = $taskSize
      batchSize = $batchSize
    }
    """.stripMargin
  )
}

class MongoRichQueueSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("MongoRichQueueSpecSystem",
    MongoRichQueueSpec.config.withFallback(PersistanceSpecConfiguration.config))
  )

  override def afterAll {
    //cleanup
    import com.mongodb.casbah.MongoClient
    import com.mongodb.casbah.Imports._

    val conf = system.settings.config
    var mongoClient = MongoClient(conf.getString("queue.mongodb.host"), conf.getInt("queue.mongodb.port"))
    var db = mongoClient(conf.getString("queue.mongodb.database"))
    var col = db(conf.getString("queue.mongodb.queue"))

    db.dropDatabase
    system.shutdown()
  }

  import vkcrawler.bfs._
  import RichQueueSpec._
  import vkcrawler.Common._
  import ReliableMessaging._
  import vkcrawler.DataModel._

  "MongoRichQueueActor " must {
    "correct return items in task " in {
      class TestRichQueueActor extends ReliableRichQueueActor {
        class MongoBackendActor extends RichQueueBackendActor with MongoRichQueueBackend
        override def createBackend = new MongoBackendActor
        override val demandThreshold = 2
      }
      val queue = system.actorOf(Props(new TestRichQueueActor))

      val ids1 = (for(i <- 1l to taskSize) yield {i})
      val ids2 = (for(i <- 1l to taskSize) yield {i+taskSize})

      queue ! RichQueue.Push(ids1.toSeq)
      queue ! RichQueue.Push(ids2.toSeq)

      queue ! RichQueue.Pop(List("task1"))
      val e2 = expectMsgClass(classOf[Envelop])
      e2.msg should be (RichQueue.Item(Task("task1", ids1)))
      queue ! Confirm(e2.deliveryId)

      queue ! RichQueue.Pop(List("task1"))
      val e3 = expectMsgClass(classOf[Envelop])
      e3.msg should be (RichQueue.Item(Task("task1", ids2)))
      queue ! Confirm(e3.deliveryId)

      queue ! RichQueue.Pop(List("task2"))
      val e4 = expectMsgClass(classOf[Envelop])
      e4.msg should be (RichQueue.Item(Task("task2", ids1)))
      queue ! Confirm(e4.deliveryId)

      queue ! RichQueue.Pop(List("task2"))
      val e5 = expectMsgClass(classOf[Envelop])
      e5.msg should be (RichQueue.Item(Task("task2", ids2)))
      queue ! Confirm(e5.deliveryId)

      queue ! RichQueue.Pop(List("task1"))
      val e6 = expectMsgClass(classOf[Envelop])
      e6.msg should be (RichQueue.Item(Task("task1", ids1)))
      queue ! Confirm(e6.deliveryId)
    }
  }
}
