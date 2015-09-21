package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe

class ExchangeSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("ExchangeSystem"))

  import vkcrawler.bfs._

  "DummyExchange" must {
    "send friends to BFS and Queue actor" in {
      val bfs = TestProbe()
      val queue = TestProbe()

      import vkcrawler.Common._

      class DummyExchange extends ExchangeActor(bfs.ref.path, queue.ref.path) with DummyExchangeBackend

      val exchange = system.actorOf(Props(new DummyExchange))
      val friends = BFS.Friends(1, Seq[VKID](2,3,4))
      exchange ! friends

      val newUsers = BFS.NewUsers(Seq[VKID](2,3,4))
      exchange ! newUsers

      bfs.expectMsg(friends)
      queue.expectMsg(RichQueue.Push(Seq[VKID](2,3,4)))
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
      import vkcrawler.Common._

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

      import vkcrawler.Common._
      val friends = BFS.Friends(1, Seq[VKID](2,3,4))
      exchange ! friends
      val newUsers = BFS.NewUsers(Seq[VKID](2,3,4))
      exchange ! newUsers

      bfs.expectMsg(friends)
      queue.expectMsg(RichQueue.Push(Seq[VKID](2,3,4)))

      //expectNoMsg(500.milliseconds) //sync

      published should be (TrieMap("friends" -> friends, "new_users" -> newUsers))
    }
  }
}
