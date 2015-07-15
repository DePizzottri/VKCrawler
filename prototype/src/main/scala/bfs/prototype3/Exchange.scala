package vkcrawler.bfs.prototype3

import akka.actor._
import akka.persistence._

import com.typesafe.config.ConfigFactory

import Common._


object Exchange {
  
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
      context.actorSelection(queueActorPath) ! msg
      //publish
      publish("new_users", msg)
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
      deliver(msg, queueActorPath)
      //publish
      publish("new_users", msg)
    }
  }

  override def preStart(): Unit = {
    init
    super.preStart()
  }
}