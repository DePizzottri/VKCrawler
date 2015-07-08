package vkcrawler.bfs.prototype3

import akka.actor._
import akka.persistence._

/*
  BFS Actor
  process guaranee -
  delivery guarantee -
*/

import Common._

object BFS {
  sealed trait Cmd
  case class Friends(user:VKID, friends:Seq[VKID]) extends Cmd
  case class InsertAndFilter(user:VKID, friends:Seq[VKID]) extends Cmd
  case class NewUsers(users:Seq[VKID]) extends Cmd
  case class Filtered(user:VKID, newFirends:Seq[VKID]) extends Cmd
}

class BFSActor extends Actor {
  import BFS._

  def receive = {
    case Friends(u, f) => {
      context.actorSelection("akka://bfs-system/user/graph") ! Friends(u, f)
      context.actorSelection("akka://bfs-system/user/used") ! InsertAndFilter(u, f)
    }
    
    case Filtered(u, f) => {
      context.actorSelection("akka://bfs-system/user/exchange") ! NewUsers(f)
    }
  }
}

import akka.persistence._

/*
  BFS Actor
  Process guaranee + (resend on failure)
  Delivery guarantee + (at-least-once)
  No snapshots
*/


class ReliableBFSActor extends ReliableMessaging {
  import BFS._

  override def persistenceId: String = "bfs-actor-id-" + self.path

  override def processCommand: Receive = 
  {
    case Friends(u, f) => {
      deliver(Friends(u, f), ActorPath.fromString("akka://bfs-system/user/graph"))
      deliver(InsertAndFilter(u, f), ActorPath.fromString("akka://bfs-system/user/used"))
    }

    case Filtered(u, f) => {
      deliver(NewUsers(f), ActorPath.fromString("akka://bfs-system/user/exchange"))
    }
  }
}