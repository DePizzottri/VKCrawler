package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

class ReliableMessagingSpec(_system:ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("ReliableMessagingSystem", PersistanceSpecConfiguration.config))

  import vkcrawler.bfs._

  case class TestMsg(data:String)

  class EvelopReceivePreCActor extends Actor with EnvelopReceive {
    override def receive = openEnvelopAndPreConfirm andThen {
      case msg => sender ! TestMsg("dataTst"); sender ! msg
    }
  }

  class EvelopReceivePostCActor extends Actor with EnvelopReceive {
    override def receive = withPostConfirmation({
      case msg => sender ! TestMsg("dataTst"); sender ! msg
    })
  }

  "EnvelopReceive " must {
    "forward non-enveloped messages " in {
      val era = system.actorOf(Props(new EvelopReceivePreCActor))

      era! TestMsg("data1")
      expectMsg(TestMsg("dataTst"))
      expectMsg(TestMsg("data1"))
    }

    "open enveloped messages with PreConfirm " in {
      val era = system.actorOf(Props(new EvelopReceivePreCActor))

      era ! ReliableMessaging.Envelop(1, TestMsg("data1"))

      expectMsg(ReliableMessaging.Confirm(1))
      expectMsg(TestMsg("dataTst"))
      expectMsg(TestMsg("data1"))
    }

    "open enveloped messages with PostConfirm " in {
      val era = system.actorOf(Props(new EvelopReceivePostCActor))

      era ! ReliableMessaging.Envelop(1, TestMsg("data1"))

      expectMsg(TestMsg("dataTst"))
      expectMsg(TestMsg("data1"))
      expectMsg(ReliableMessaging.Confirm(1))
    }
  }

  class ReliableEchoMessager extends ReliableMessaging {
    override def persistenceId: String = "test-id"

    override def processCommand: Receive = {
      case msg => sender ! msg; sender ! TestMsg("data2")
    }
  }

  "ReliableMessaging " must {
    "process ordinary message " in {
      val rem = system.actorOf(Props(new ReliableEchoMessager))

      rem ! TestMsg("data1")
      expectMsg(TestMsg("data1"))
      expectMsg(TestMsg("data2"))
    }

    "confirm delivered message " in {
      val rem = system.actorOf(Props(new ReliableEchoMessager))

      rem ! ReliableMessaging.Envelop(1, TestMsg("data1"))
      expectMsg(TestMsg("data1"))
      expectMsg(TestMsg("data2"))
      expectMsgClass(classOf[ReliableMessaging.Confirm]).deliveryId should be (1)
    }

    "redeliver unconfirmed messges after stop/start" in {
      class QuickReliableEchoMessager extends ReliableEchoMessager {
        override def redeliverInterval = 1.seconds
        override def processCommand: Receive = {
          case "stop" => system.stop(self)
          case msg => deliver(msg, sender.path)
        }
      }

      val rem = system.actorOf(Props(new QuickReliableEchoMessager))
      val msg = TestMsg("data")

      import ReliableMessaging._

      rem ! msg
      rem ! msg
      rem ! msg

      {
        val msgs1 = receiveN(3,  1.seconds)
        val ids1 = msgs1.map(x => x.asInstanceOf[Envelop].deliveryId).toSet
        ids1.size should be (3)
        ids1.contains(2) should be (true)
        rem ! Confirm(2)
      }

      rem ! "stop"

      expectNoMsg(3.seconds)

      val rem1 = system.actorOf(Props(new QuickReliableEchoMessager))

      val msgs2 = receiveN(4, 2.seconds)
      val ids2 = msgs2.map(x => x.asInstanceOf[Envelop].deliveryId).toSet
      ids2.size should be (2)

      ids2.foreach{x => rem1 ! Confirm(x)}

      expectNoMsg(3.seconds)
    }
  }

  "Hierarchical snapshot " must {
    "save snapshots correctly" in {
      import ReliableMessaging._

      class Lvl1Actor extends ReliableMessaging {
        override def persistenceId: String = "hier-snapshot-test-id"
        override def redeliverInterval = 1.seconds
        override def processCommand: Receive = {
          case "get1" => sender ! state1
        }
        var state1 = ""
        override def storeSnapshot(childStates: List[Any]):Unit = {
          super.storeSnapshot("LVL1" :: childStates)
        }
        override def restoreFromSnapshot(states:List[Any]): Unit = {
          println(s"Lvl1 restore $states")
          state1 = states.head.asInstanceOf[String]
          super.restoreFromSnapshot(states.tail)
        }
      }

      class Lvl2Actor extends Lvl1Actor {
        var state2 = "0xdeadbeef"
        override def processCommand:Receive = super.processCommand orElse {
          case "get2" => sender ! state2
          case s:String => deliver(s+s, sender.path)
        }

        override def storeSnapshot(childStates: List[Any]):Unit = {
          super.storeSnapshot("LVL2" :: childStates)
        }
        override def restoreFromSnapshot(states:List[Any]): Unit = {
          state2 = states.head.asInstanceOf[String]
          super.restoreFromSnapshot(states.tail)
        }
      }

      val act1 = system.actorOf(Props(new Lvl2Actor), "snap-actor-1")
      val msg = "msgmsg"
      act1 ! msg
      expectMsg(Envelop(1, msg+msg))

      act1 ! HierarchicalSnapshotStore.SaveSnapshot
      act1 ! PoisonPill

      expectNoMsg(2.seconds)

      val act2 = system.actorOf(Props(new Lvl2Actor), "snap-actor-2")
      expectMsg(Envelop(1, msg+msg))
      act2 ! Confirm(1)

      act2 ! "get1"
      expectMsg("LVL1")
      act2 ! "get2"
      expectMsg("LVL2")
    }

    "call default save snapshot" in {
      import ReliableMessaging._
      class TestActor extends ReliableMessaging {
        override def persistenceId: String = "def-snapshot-test-id"
        override def redeliverInterval = 1.seconds
        override def processCommand: Receive = {
          case s:String => deliver(s+s, sender.path)
        }
      }

      val act1 = system.actorOf(Props(new TestActor), "def-snap-actor-1")
      val msg = "msgdata"
      act1 ! msg
      expectMsg(Envelop(1, msg+msg))

      //no confirmation

      act1 ! HierarchicalSnapshotStore.SaveSnapshot
      act1 ! PoisonPill

      expectNoMsg(3.seconds)

      val act2 = system.actorOf(Props(new TestActor), "def-snap-actor-2")
      expectMsg(Envelop(1, msg+msg))
      act2 ! Confirm(1)
    }
  }
}
