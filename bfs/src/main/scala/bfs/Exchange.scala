package vkcrawler.bfs

import akka.actor._
import akka.persistence._

import com.typesafe.config.ConfigFactory

import vkcrawler.Common._
import spray.json._
import vkcrawler.bfs.SprayJsonSupport._

object Exchange {
  case class Publish(key:String, data:JsValue)
}

class ExchangeActor(bfsActorPath:ActorPath, queueActorPath:ActorPath) extends Actor {
  this: ExchangeBackend =>

  override def preStart(): Unit = {
    init
    super.preStart()
  }

  override def receive = {
    case msg@BFS.Friends(id, ids) => {
      context.actorSelection(bfsActorPath) ! msg
      //publish
      import FriendsJsonSupport._
      publish("friends", msg.toJson)
    }
    case msg@BFS.NewUsers(ids) => {
      context.actorSelection(queueActorPath) ! RichQueue.Push(ids)
      //publish
      import NewUsersJsonSupport._
      publish("new_users", msg.toJson)
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
      import FriendsJsonSupport._
      publish("friends", msg.toJson)
    }
    case msg@BFS.NewUsers(ids) => {
      deliver(RichQueue.Push(ids), queueActorPath)
      //publish
      import NewUsersJsonSupport._
      publish("new_users", msg.toJson)
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
