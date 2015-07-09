package vkcrawler.bfs.prototype3

import akka.actor._
import akka.persistence._

import Common._


object Exchange {
  
}

class ExchangeActor extends Actor {
  this: ExchangeBackend =>
  
  init

  override def receive = {
    case msg@BFS.Friends(id, ids) => {
      context.actorSelection("akka://bfs-system/user/bfs") ! msg
      //publish
      publish("friends", msg)
    }
    case msg@BFS.NewUsers(ids) => {
      context.actorSelection("akka://bfs-system/user/queue") ! msg
      //publish
      publish("new_users", msg)
    }
  }
}

class ReliableExchangeActor extends ReliableMessaging {
  this: ExchangeBackend =>

  override def persistenceId: String = "exchange-actor-id-" + self.path

  override def processCommand: Receive = 
  {
    case msg@BFS.Friends(id, ids) => {
      deliver(msg, ActorPath.fromString("akka://bfs-system/user/bfs"))
      //publish
      publish("friends", msg)
    }
    case msg@BFS.NewUsers(ids) => {
      deliver(msg, ActorPath.fromString("akka://bfs-system/user/queue"))
      //publish
      publish("new_users", msg)
    }
  }

  override def preStart(): Unit = {
    init
  }
}