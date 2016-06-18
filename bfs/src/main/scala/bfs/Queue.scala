package vkcrawler.bfs

import akka.actor._
import akka.persistence._

import vkcrawler.Common._

import vkcrawler.DataModel._

object Queue {
  case class Push(ids:Seq[VKID])
  case class Pop(`type`:String)
  case object PopNoConfirm
  case class Item(task:Task)
}

/*
  Queue actor
  process guaranee -
  delivery guarantee -
*/

class QueuePopActor extends Actor {
  this: QueueBackend =>
  import Queue._

  def receive = {
    case Pop(t) => {
      val task = pop(t)
      sender ! Item(task)
    }
  }
}

class QueuePushActor extends Actor {
  this: QueueBackend =>
  import Queue._

  def receive = {
    case Push(ids) => {
      push(ids)
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
    case Pop(t) => {
      val task = pop(t)
      deliver(Item(task), sender.path)
    }
  }

  override def preStart(): Unit = {
    recoverQueue
    super.preStart()
  }
}
