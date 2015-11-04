package vkcrawler.bfs

import akka.actor._
import akka.persistence._

import vkcrawler.Common._

object Queue {
  case class Push(ids:Seq[VKID])
  case class Pop()
  case class Items(ids:Seq[VKID])
  case class Empty()
}

/*
  Queue actor
  process guaranee -
  delivery guarantee -
*/

class QueueActor extends Actor {
  this: QueueBackend =>
  import Queue._

  def receive = {
    case Push(ids) => {
      push(ids)
    }
    case Pop => {
      val items = popMany()
      if(!items.isEmpty)
        sender ! Items(items)
      else
        sender ! Empty
    }
  }
}

/*
  Queue actor
  Process guaranee + (resend on failure)
  Delivery guarantee + (at-least-once)
  No snapshots
*/

class ReliableQueueActor extends ReliableMessaging {
  this: ReliableQueueBackend =>

  import Queue._

  override def persistenceId: String = "queue-actor-id"

  override def processCommand: Receive =
  {
    case Push(ids) => {
      push(ids)
    }
    case Pop => {
      val items = popMany()
      if(!items.isEmpty) {
        deliver(Items(items), sender.path)
      } else {
        deliver(Empty, sender.path)
      }
    }
  }

  override def preStart(): Unit = {
    recoverQueue
    super.preStart()
  }
}
