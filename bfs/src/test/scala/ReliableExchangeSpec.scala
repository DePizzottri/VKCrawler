package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe

object ReliableExchangeSpec {
  def config = ConfigFactory.parseString(
    """
    akka {
      persistence {
        at-least-once-delivery {
          redeliver-interval = 1s
        }
      }
    }
    """.stripMargin
  )
}

class ReliableExchangeSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("ExchangeSystem", ReliableExchangeSpec.config.withFallback(PersistanceSpecConfiguration.config)))

  import vkcrawler.bfs._

  assume(system.settings.config.getString("akka.persistence.at-least-once-delivery.redeliver-interval") === "1s")

  "ReliableDummyExchange" must {
    "redeliver unconfimed messages" in {
      val bfs = TestProbe()
      val queue = TestProbe()

      import vkcrawler.Common._

      class ReliableDummyExchange extends ReliableExchangeActor(bfs.ref.path, queue.ref.path) with DummyExchangeBackend

      val exchange = system.actorOf(Props(new ReliableDummyExchange))

      val friends = BFS.Friends(1, Seq[VKID](2,3,4))
      exchange ! friends
      import ReliableMessaging._
      val ebsf = bfs.expectMsgClass(classOf[Envelop])
      ebsf.msg should be (friends)
      bfs.receiveN(2, 3.seconds)
      exchange ! Confirm(ebsf.deliveryId)

      val newUsers = BFS.NewUsers(Seq[VKID](2,3,4))
      exchange ! newUsers

      val eq = queue.expectMsgClass(classOf[Envelop])
      eq.msg should be (Queue.Push(newUsers.users))
      queue.receiveN(2, 3.seconds)
      exchange ! Confirm(eq.deliveryId)
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

  "ReliableDummyExchange " must {
    "accept with confirm, both send to BFS and Queue and publish" in {
      val bfs = TestProbe()
      val queue = TestProbe()

      class ReliableDummyExchange extends ReliableExchangeActor(bfs.ref.path, queue.ref.path) with MockExchangeBackend
      val exchange = system.actorOf(Props(new ReliableDummyExchange))

      import vkcrawler.Common._
      val friends = BFS.Friends(1, Seq[VKID](2,3,4))
      exchange ! friends

      import ReliableMessaging._
      val ebsf = bfs.expectMsgClass(classOf[Envelop])
      ebsf.msg should be (friends)
      exchange ! Confirm(ebsf.deliveryId)

      val newUsers = BFS.NewUsers(Seq[VKID](2,3,4))
      exchange ! newUsers
      val eq = queue.expectMsgClass(classOf[Envelop])
      eq.msg should be (Queue.Push(newUsers.users))
      exchange ! Confirm(eq.deliveryId)

      published should be (TrieMap("friends" -> friends, "new_users" -> newUsers))
    }
  }
}
