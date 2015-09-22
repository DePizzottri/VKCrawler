package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random

import akka.actor._
import scala.concurrent.duration._

object RichQueuePersitenceSpec {
  val taskSize = 5
  val batchSize = 10

  def config = ConfigFactory.parseString(
    s"""
    queue {
      taskSize = $taskSize
      batchSize = $batchSize
    }
    """.stripMargin
  )
}

class RichQueuePersitenceSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("RichQueuePersitenceSpecSystem",
    RichQueuePersitenceSpec.config.withFallback(PersistanceSpecConfiguration.config))
  )

  import vkcrawler.bfs._
  import RichQueueSpec._
  import vkcrawler.Common._
  import ReliableMessaging._
  import vkcrawler.DataModel._

  "ReliableRichQueueActor " must {
    "restore local queues on restart " in {
      class TestRichQueueActor extends ReliableRichQueueActor {
        class LocalBackendActor extends RichQueueBackendActor with LocalRichQueueBackend
        override def persistenceId = "test-rich-queue-id2"
        override def createBackend = new LocalBackendActor
        override val demandThreshold = 2
      }

      val queue1 = system.actorOf(Props(new TestRichQueueActor), "Queue1")
      val idsAll = (for(i <- 1l to taskSize*2) yield {i})
      val idsGr = idsAll.grouped(taskSize).toArray

      queue1 ! RichQueue.Push(idsAll)

      queue1 ! RichQueue.Pop(List("task1"))
      val e1 = expectMsgClass(classOf[Envelop])
      e1.msg should be (RichQueue.Item(Task("task1", idsGr(0))))
      queue1 ! Confirm(e1.deliveryId)

      queue1 ! RichQueue.Pop(List("task2"))
      val e2 = expectMsgClass(classOf[Envelop])
      e2.msg should be (RichQueue.Item(Task("task2", idsGr(0))))
      queue1 ! Confirm(e2.deliveryId)

      //queue1 ! HierarchicalSnapshotStore.SaveSnapshot()
      //expectNoMsg(1.seconds)
      queue1 ! PoisonPill

      val queue2 = system.actorOf(Props(new TestRichQueueActor), "Queue2")

      queue2 ! RichQueue.Pop(List("task1"))
      val e3 = expectMsgClass(classOf[Envelop])
      e3.msg should be (RichQueue.Item(Task("task1", idsGr(1))))
      queue2 ! Confirm(e3.deliveryId)

      queue2 ! RichQueue.Pop(List("task2"))
      val e4 = expectMsgClass(classOf[Envelop])
      e4.msg should be (RichQueue.Item(Task("task2", idsGr(1))))
      queue2 ! Confirm(e4.deliveryId)
    }
  }
}
