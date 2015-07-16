package vkcrawler.bfs.prototype3.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe

class ReliableBFSSpec(_system: ActorSystem) extends BFSTestSpec(_system) {
  def this() = this(ActorSystem("ReliableBFSSpecSystem", PersistanceSpecConfiguration.config))

  import vkcrawler.bfs.prototype3._

  "ReliableBFS actor" must {
    "route Friends and NewUsers messages" in {
      val graph = TestProbe()
      val used = TestProbe()
      val exchange = TestProbe()

      val bfs = system.actorOf(Props(new ReliableBFSActor(graph.ref.path, used.ref.path, exchange.ref.path) {
        override def redeliverInterval = 1.seconds
      }))

      import Common._
      import BFS._
      import ReliableMessaging._

      val friends = Friends(1, Seq[VKID](1, 2, 3))
      val filtered = Filtered(1, Seq[VKID](4, 5, 6))

      bfs ! Envelop(1, friends)
      expectMsg(Confirm(1))

      val e1 = graph.expectMsgClass(classOf[Envelop])
      e1.msg should be (friends)
      bfs ! Confirm(e1.deliveryId)

      val e2 = used.expectMsgClass(classOf[Envelop])
      e2.msg should be (InsertAndFilter(friends.user, friends.friends))
      bfs ! Confirm(e2.deliveryId)

      bfs ! filtered
      val e3 = exchange.expectMsgClass(classOf[Envelop])
      e3.msg should be (NewUsers(filtered.newFriends))

      exchange.expectMsgClass(classOf[Envelop]) should be (e3)
      bfs ! Confirm(e3.deliveryId)
    }
  }
}
