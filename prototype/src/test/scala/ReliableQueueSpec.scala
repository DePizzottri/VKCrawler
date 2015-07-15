package vkcrawler.bfs.prototype3.test

import akka.actor._
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._
 
class ReliableLocalQueueSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("ReliableLocalQueueSystem", PersistanceSpecConfiguration.config))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  import vkcrawler.bfs.prototype3._

  class ReliableLocalQueue extends ReliableQueueActor with ReliableLocalQueueBackend
 
  "ReliableLocalQueueActor " must {
    "redeliver unconfirmed message " in {
      import ReliableMessaging._
      val queue = system.actorOf(Props(new ReliableLocalQueue))

      queue ! Envelop(11, Queue.Pop)
      
      expectMsg(Confirm(11))
      val e = expectMsgClass(classOf[Envelop])
      e.msg should be (Queue.Empty)
      
      receiveN(2, 15.seconds)
      
      queue ! Confirm(e.deliveryId)
    }
    
    "return empty on emtpy queue" in {
      import ReliableMessaging._
      val queue = system.actorOf(Props(new ReliableLocalQueue))
      queue ! Envelop(13, Queue.Pop)

      expectMsg(Confirm(13))
      val e = expectMsgClass(classOf[Envelop])
      e.msg should be (Queue.Empty)
      queue ! Confirm(e.deliveryId)
      expectNoMsg(1.seconds)
    }

    "preserve queue order" in {
      import Common._
      import ReliableMessaging._
      val queue = system.actorOf(Props(new ReliableLocalQueue))
      val ins = Seq[VKID](1, 2, 3, 4)
      queue ! Envelop(1, Queue.Push(ins))
      expectMsg(Confirm(1))
      
      queue ! Envelop(2, Queue.Pop)
      expectMsg(Confirm(2))
      expectMsgClass(classOf[Envelop]).msg should be (Queue.Items(Seq(1)))
      
      queue ! Queue.Pop
      expectMsgClass(classOf[Envelop]).msg should be (Queue.Items(Seq(2)))

      queue ! Envelop(3, Queue.Pop)
      expectMsg(Confirm(3))
      expectMsgClass(classOf[Envelop]).msg should be (Queue.Items(Seq(3)))

      queue ! Envelop(4, Queue.Pop)
      expectMsg(Confirm(4))
      expectMsgClass(classOf[Envelop]).msg should be (Queue.Items(Seq(4)))
    }    
    
    "then redeliver unconfirmed messages" in {
      import Common._
      import ReliableMessaging._
      val queue = system.actorOf(Props(new ReliableLocalQueue))
      
      def expectAndConfirm(id:VKID) {
        val envlp = expectMsgClass(classOf[Envelop])
        envlp.msg should be (Queue.Items(Seq(id)))
        
        queue ! Confirm (envlp.deliveryId)
      }
      
      expectAndConfirm(1)
      expectAndConfirm(2)
      expectAndConfirm(3)
      expectAndConfirm(4)
    }
  }
}