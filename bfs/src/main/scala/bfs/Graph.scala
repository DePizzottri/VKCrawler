package vkcrawler.bfs

import akka.actor._

object Graph {
}

class GraphActor extends Actor {
  this: GraphSaverBackend =>

  override def receive = {
    case BFS.Friends(u, f) => {
      saveFriends(u, f)
    }
  }
}

class ReliableGraphActor extends Actor with EnvelopReceive {
  this: ReliableGraphSaverBackend =>

  override def receive = withPostConfirmation {
    case BFS.Friends(u, f) => {
      saveFriends(u, f)
    }
  }

  override def preStart(): Unit = {
    init
    super.preStart()
  }
}
