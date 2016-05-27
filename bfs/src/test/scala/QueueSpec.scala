package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._

class QueueSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("QueueSpecSystem"))

  import vkcrawler.bfs._
  import vkcrawler.DataModel._

  class LocalQueueActor extends QueueActor with LocalQueueBackend

  "LocalQueueActor " must {
    "return empty on emtpy queue" in {
      val queue = system.actorOf(Props(new LocalQueueActor))
      queue ! Queue.Pop("stub")
      expectMsg(Queue.Item(Task("stub", Seq())))
    }

    "preserve queue order" in {
      import vkcrawler.Common._
      val queue = system.actorOf(Props(new LocalQueueActor))
      val ins = Seq[VKID](1, 2, 3, 4)
      queue ! Queue.Push(ins)
      queue ! Queue.Pop("stub")
      expectMsg(Queue.Item(Task("stub", Seq(TaskData(1, None)))))
      queue ! Queue.Pop("stub")
      queue ! Queue.Pop("stub")
      queue ! Queue.Pop("stub")
      expectMsg(Queue.Item(Task("stub", Seq(TaskData(2, None)))))
      expectMsg(Queue.Item(Task("stub", Seq(TaskData(3, None)))))
      expectMsg(Queue.Item(Task("stub", Seq(TaskData(4, None)))))
    }
  }
}
