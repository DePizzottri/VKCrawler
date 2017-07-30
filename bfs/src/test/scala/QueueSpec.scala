package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._

class QueueSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("QueueSpecSystem"))

  import vkcrawler.bfs._
  import vkcrawler.DataModel._

  class LocalQueuePopActor extends QueuePopActor with LocalQueueBackend
  class LocalQueuePushActor extends QueuePushActor with LocalQueueBackend

  "LocalQueueActor " must {
    "return empty on emtpy queue" in {
      val queue = system.actorOf(Props(new LocalQueuePopActor))
      queue ! Queue.Pop("stub")
      expectMsg(Queue.Item(Task("stub", Seq())))
    }

    // "preserve queue order" in {
    //   import vkcrawler.Common._
    //   val popQueue = system.actorOf(Props(new LocalQueuePopActor))
    //   val pushQueue = system.actorOf(Props(new LocalQueuePushActor))
    //   val ins = Seq[VKID](1, 2, 3, 4)
    //   pushQueue ! Queue.Push(ins)
    //   popQueue ! Queue.Pop("stub")
    //   expectMsg(Queue.Item(Task("stub", Seq(TaskData(1, None)))))
    //   popQueue ! Queue.Pop("stub")
    //   popQueue ! Queue.Pop("stub")
    //   popQueue ! Queue.Pop("stub")
    //   expectMsg(Queue.Item(Task("stub", Seq(TaskData(2, None)))))
    //   expectMsg(Queue.Item(Task("stub", Seq(TaskData(3, None)))))
    //   expectMsg(Queue.Item(Task("stub", Seq(TaskData(4, None)))))
    // }
  }
}
