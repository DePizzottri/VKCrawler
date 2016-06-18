package vkcrawler.bfs

import akka.actor._
import akka.persistence._

/*
  BFS Actor
  process guaranee -
  delivery guarantee -
*/

import vkcrawler.Common._

object BFS {
  sealed trait Cmd
  case class Friends(user:VKID, friends:Seq[VKID]) extends Cmd
  case class NewUsers(users:Seq[VKID]) extends Cmd
}

class BFSActor(graph:ActorPath, used:ActorPath, exchange:ActorPath) extends Actor {
  import BFS._

  def receive = {
    case Friends(u, f) => {
      //context.actorSelection(graph) ! Friends(u, f)
      context.actorSelection(used) ! Used.InsertAndFilter(f)
    }

    case Used.Filtered(f) => {
      context.actorSelection(exchange) ! NewUsers(f)
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


class ReliableBFSActor(graph:ActorPath, used:ActorPath, exchange:ActorPath) extends ReliableMessaging {
  import BFS._

  override def persistenceId: String = "bfs-actor-id"

  override def processCommand: Receive =
  {
    case Friends(u, f) => {
      deliver(Friends(u, f), graph)
      deliver(Used.InsertAndFilter(f), used)
    }

    case Used.Filtered(f) => {
      deliver(NewUsers(f), exchange)
    }
  }
}
