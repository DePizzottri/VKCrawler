package vkcrawler.bfs.prototype3.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe

class ReliableGraphSpec(_system: ActorSystem) extends BFSTestSpec(_system) {
  def this() = this(ActorSystem("ReliableGraphSpecSystem"))

  import vkcrawler.bfs.prototype3._

  "ReliableGraphActor " must {
    "Save friends list" in {
      import Common._
      import scala.collection.mutable.Map
      var fr = Map.empty[VKID, Seq[VKID]]
      class DummyReliableGraphActor extends ReliablGraphActor with ReliableGraphSaverBackend {
        def init():Unit = {
          fr.clear
        }
        def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
          fr += (id -> ids)
        }
      }

      import ReliableMessaging._
      val graph = system.actorOf(Props(new DummyReliableGraphActor))
      val friends = BFS.Friends(1, Seq[VKID](1, 2, 3))

      graph ! Envelop(1, friends)

      expectMsg(Confirm(1))

      fr should be (Map(friends.user -> friends.friends))
    }
  }
}
