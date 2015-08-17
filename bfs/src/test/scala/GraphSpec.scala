package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe

class GraphSpec(_system: ActorSystem) extends BFSTestSpec(_system) {
  def this() = this(ActorSystem("GraphSpecSystem"))

  import vkcrawler.bfs.prototype3._

  "GraphActor " must {
    "Save friends list" in {
      import Common._
      import scala.collection.mutable.Map
      class DummyGraphActor extends GraphActor with GraphSaverBackend {
        var fr = Map.empty[VKID, Seq[VKID]]

        def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
          fr += (id -> ids)
        }

        override def receive: Receive = super.receive orElse {
          case "get" => sender ! fr
        }
      }

      val graph = system.actorOf(Props(new DummyGraphActor))
      val friends = BFS.Friends(1, Seq[VKID](1, 2, 3))

      graph ! friends

      graph ! "get"

      expectMsg(Map(friends.user -> friends.friends))
    }
  }
}
