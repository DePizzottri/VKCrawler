package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._

class LocalQueueSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("QueueSpecSystem"))

  import vkcrawler.bfs.prototype3._

  class LocalQueueActor extends QueueActor with LocalQueueBackend

  "LocalQueueActor " must {
    "return empty on emtpy queue" in {
      val queue = system.actorOf(Props(new LocalQueueActor))
      queue ! Queue.Pop
      expectMsg(Queue.Empty)
    }

    "preserve queue order" in {
      import Common._
      val queue = system.actorOf(Props(new LocalQueueActor))
      val ins = Seq[VKID](1, 2, 3, 4)
      queue ! Queue.Push(ins)
      queue ! Queue.Pop
      expectMsg(Queue.Items(Seq(1)))
      queue ! Queue.Pop
      queue ! Queue.Pop
      queue ! Queue.Pop
      expectMsg(Queue.Items(Seq(2)))
      expectMsg(Queue.Items(Seq(3)))
      expectMsg(Queue.Items(Seq(4)))
    }
  }
}
