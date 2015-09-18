package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random


import akka.actor._
import scala.concurrent.duration._

object RichQueueSpec {
  val taskSize = 5
  val batchSize = 1

  def config = ConfigFactory.parseString(
    s"""
    queue {
      taskSize = $taskSize
      batchSize = $batchSize
    }
    """.stripMargin
  )
}

class RichQueueSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("RichQueueSpecSystem",
    RichQueueSpec.config.withFallback(PersistanceSpecConfiguration.config))
  )

  import vkcrawler.bfs._
  import RichQueueSpec._
  import vkcrawler.Common._
  import ReliableMessaging._
  import vkcrawler.DataModel._

  "ReliableRichQueueActor " must {
    "correct return items in task " in {
      class TestRichQueueActor extends ReliableRichQueueActor {
        override val demandThreshold = 0
      }

      val queue = system.actorOf(Props(new TestRichQueueActor))

      val ids1 = (for(i <- 1l to taskSize) yield {i})
      val ids2 = (for(i <- 1l to taskSize) yield {i+taskSize})

      queue ! RichQueue.Push(ids1.toSeq)
      queue ! RichQueue.Push(ids2.toSeq)

      queue ! RichQueue.Pop(List("task1"))
      val e1 = expectMsgClass(classOf[Envelop])
      e1.msg should be (RichQueue.Empty)
      queue ! Confirm(e1.deliveryId)

      expectNoMsg(1.seconds)

      queue ! RichQueue.Pop(List("task1"))
      val e2 = expectMsgClass(classOf[Envelop])
      e2.msg should be (RichQueue.Item(Task("task1", ids1)))
      queue ! Confirm(e2.deliveryId)

      queue ! RichQueue.Pop(List("task2"))
      val e4 = expectMsgClass(classOf[Envelop])
      e4.msg should be (RichQueue.Empty)
      queue ! Confirm(e4.deliveryId)

      expectNoMsg(1.seconds)

      queue ! RichQueue.Pop(List("task2"))
      val e3 = expectMsgClass(classOf[Envelop])
      e3.msg should be (RichQueue.Item(Task("task2", ids2)))
      queue ! Confirm(e3.deliveryId)
    }

    // "return empty on emtpy queue" in {
    // }
    //
    // "preserve queue order" in {
    // }
    //
    // "then redeliver unconfirmed messages" in {
    // }
  }
}
