package vkcrawler.bfs.prototype3.test

import akka.actor._
import akka.testkit.{TestActors, TestKit, ImplicitSender}
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Assertions._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

class ReliableMessagingSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("ReliableMessagingSystem", PersistanceSpecConfiguration.config))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  import vkcrawler.bfs.prototype3._

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
}
