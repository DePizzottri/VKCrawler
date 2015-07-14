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
    
    override def processRecover: Receive = {
      case msg:akka.persistence.RecoveryCompleted => Unit
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
  }
}