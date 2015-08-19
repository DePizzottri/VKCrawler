package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe

class BFSSpec(_system: ActorSystem) extends BFSTestSpec(_system) {
  def this() = this(ActorSystem("BFSSpecSystem"))

  import vkcrawler.bfs._

  "BFS actor" must {
    "route Friends and NewUsers messages" in {
      val graph = TestProbe()
      val used = TestProbe()
      val exchange = TestProbe()

      val bfs = system.actorOf(Props(new BFSActor(graph.ref.path, used.ref.path, exchange.ref.path)))

      import vkcrawler.Common._
      import BFS._

      val friends = Friends(1, Seq[VKID](1, 2, 3))
      val filtered = Used.Filtered(Seq[VKID](4, 5, 6))

      bfs ! friends

      graph.expectMsg(friends)
      used.expectMsg(Used.InsertAndFilter(friends.friends))

      bfs ! filtered
      exchange.expectMsg(NewUsers(filtered.ids))
    }
  }
}
