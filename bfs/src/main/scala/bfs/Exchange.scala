package vkcrawler.bfs

import akka.actor._
import akka.persistence._

import com.typesafe.config.ConfigFactory

import vkcrawler.Common._

object Exchange {
  case class Publish(key:String, data:String)
}

class ExchangeActor(bfsActorPath:ActorPath, queueActorPath:ActorPath) extends Actor {
  this: ExchangeBackend =>

  init

  override def receive = {
    case msg@BFS.Friends(id, ids) => {
      context.actorSelection(bfsActorPath) ! msg
      //publish
      publish("friends", msg)
    }
    case msg@BFS.NewUsers(ids) => {
      context.actorSelection(queueActorPath) ! Queue.Push(ids)
      //publish
      publish("new_users", msg)
    }

    case Exchange.Publish(key, data) => {
      publish(key, data)
    }
  }
}

class ReliableExchangeActor(bfsActorPath:ActorPath, queueActorPath:ActorPath) extends ReliableMessaging {
  this: ExchangeBackend =>

  override def persistenceId: String = "exchange-actor-id"

  override def processCommand: Receive =
  {
    case msg@BFS.Friends(id, ids) => {
      deliver(msg, bfsActorPath)
      //publish
      publish("friends", msg)
    }
    case msg@BFS.NewUsers(ids) => {
      deliver(Queue.Push(ids), queueActorPath)
      //publish
      publish("new_users", msg)
    }
    case Exchange.Publish(key, data) => {
      publish(key, data)
    }
  }

  override def preStart(): Unit = {
    init
    super.preStart()
  }
}
