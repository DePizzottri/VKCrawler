package vkcrawler.bfs.prototype3.test

import akka.actor._
import akka.testkit.{TestActors, TestKit, ImplicitSender}
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe

class ExchangeSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("ExchangeSystem"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  import vkcrawler.bfs.prototype3._

  "DummyExchange" must {
    "send friends to BFS and Queue actor" in {
      val bfs = TestProbe()
      val queue = TestProbe()
      
      import Common._      

      class DummyExchange extends ExchangeActor(bfs.ref.path, queue.ref.path) with DummyExchangeBackend

      val exchange = system.actorOf(Props(new DummyExchange))
      val friends = BFS.Friends(1, Seq[VKID](2,3,4))
      exchange ! friends

      val newUsers = BFS.NewUsers(Seq[VKID](2,3,4))
      exchange ! newUsers
      
      bfs.expectMsg(friends)
      queue.expectMsg(newUsers)
    }
  }
    
  import scala.collection.concurrent.TrieMap
    
  var published = TrieMap.empty[String, Any]
  trait MockExchangeBackend extends ExchangeBackend {
    def init:Unit = {
      published = TrieMap.empty[String, Any]
    }
    def publish(tag:String, msg: Any):Unit = {
      published += ((tag, msg))
    }
  }

    
  "PublishingExchange " must {
    "publish incoming users and new friends " in {
      import Common._      

      val bfs = TestProbe()
      val queue = TestProbe()

      class PublishingExchange extends ExchangeActor(bfs.ref.path, queue.ref.path) with MockExchangeBackend

      val exchange = system.actorOf(Props(new PublishingExchange))
      val friends = BFS.Friends(1, Seq[VKID](2,3,4))
      exchange ! friends
      val newUsers = BFS.NewUsers(Seq[VKID](2,3,4))
      exchange ! newUsers
      
      expectNoMsg(500.milliseconds) //sync
      
      published should be (TrieMap("friends" -> friends, "new_users" -> newUsers))
    }
    
    "both send to BFS and Queue and publish" in {
      val bfs = TestProbe()
      val queue = TestProbe()

      class PublishingExchange extends ExchangeActor(bfs.ref.path, queue.ref.path) with MockExchangeBackend

      val exchange = system.actorOf(Props(new PublishingExchange))
      
      import Common._
      val friends = BFS.Friends(1, Seq[VKID](2,3,4))
      exchange ! friends
      val newUsers = BFS.NewUsers(Seq[VKID](2,3,4))
      exchange ! newUsers
      
      bfs.expectMsg(friends)
      queue.expectMsg(newUsers)
      
      //expectNoMsg(500.milliseconds) //sync
      
      published should be (TrieMap("friends" -> friends, "new_users" -> newUsers))
    }
  }
}