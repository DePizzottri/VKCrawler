package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._

class ReliableLocalQueueSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("ReliableLocalQueueSystem", PersistanceSpecConfiguration.config))

  import vkcrawler.bfs._
  import vkcrawler.DataModel._

  class ReliableLocalQueue extends ReliableQueueActor with ReliableLocalQueueBackend

  "ReliableLocalQueueActor " must {
    "redeliver unconfirmed message " in {
      import ReliableMessaging._
      val queue = system.actorOf(Props(new ReliableLocalQueue))

      queue ! Envelop(11, Queue.Pop("stub"))

      expectMsg(Confirm(11))
      val e = expectMsgClass(classOf[Envelop])
      e.msg should be (Queue.Item(Task("stub", Seq())))

      receiveN(2, 15.seconds)

      queue ! Confirm(e.deliveryId)
    }

    "return empty on emtpy queue" in {
      import ReliableMessaging._
      val queue = system.actorOf(Props(new ReliableLocalQueue))
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
      val queue = system.actorOf(Props(new ReliableLocalQueue))
      val ins = Seq[VKID](1, 2, 3, 4)
      queue ! Envelop(1, Queue.Push(ins))
      expectMsg(Confirm(1))

      queue ! Envelop(2, Queue.Pop("stub"))
      expectMsg(Confirm(2))
      expectMsgClass(classOf[Envelop]).msg should be (Queue.Item(Task("stub", Seq(TaskData(1, None)))))

      queue ! Queue.Pop("stub")
      expectMsgClass(classOf[Envelop]).msg should be (Queue.Item(Task("stub", Seq(TaskData(2, None)))))

      queue ! Envelop(3, Queue.Pop("stub"))
      expectMsg(Confirm(3))
      expectMsgClass(classOf[Envelop]).msg should be (Queue.Item(Task("stub", Seq(TaskData(3, None)))))

      queue ! Envelop(4, Queue.Pop("stub"))
      expectMsg(Confirm(4))
      expectMsgClass(classOf[Envelop]).msg should be (Queue.Item(Task("stub", Seq(TaskData(4, None)))))
    }

    "then redeliver unconfirmed messages" in {
      import vkcrawler.Common._
      import ReliableMessaging._
      val queue = system.actorOf(Props(new ReliableLocalQueue))

      def expectAndConfirm(id:VKID) {
        val envlp = expectMsgClass(classOf[Envelop])
        envlp.msg should be (Queue.Item(Task("stub", Seq(TaskData(id, None)))))

        queue ! Confirm (envlp.deliveryId)
      }

      expectAndConfirm(1)
      expectAndConfirm(2)
      expectAndConfirm(3)
      expectAndConfirm(4)
    }
  }
}
