package vkcrawler.bfs

import akka.actor._

import vkcrawler.Common._

object Used {
  case class InsertAndFilter(ids:Seq[VKID])
  case class Filtered(ids:Seq[VKID])
}

/*
  Used Actor
  process guaranee -
  delivery guarantee -
*/


class UsedActor extends Actor {
  this: UsedBackend =>
  import Used._
  override def receive = {
    case InsertAndFilter(ids) => sender ! Filtered(insertAndFilter(ids))
  }
}

import akka.persistence._

object PersistentUsedActor {
  case class IncomeIds(ids: Seq[VKID])
}

class PersistentUsedActor extends PersistentActor {
  import Used._
  import PersistentUsedActor._

  var used = scala.collection.mutable.HashSet.empty[VKID]

  override def persistenceId: String = "used-actor-id"

  override def receiveCommand: Receive = {
    case InsertAndFilter(ids) => persist(IncomeIds(ids)) { evt =>
      var ret = ids.filter{x => !used.contains(x)}
      updateState(evt.ids)
      sender ! Filtered(ret)
    }
  }

  override def receiveRecover: Receive = {
    case msg@IncomeIds(ids) => updateState(ids)
  }

  def updateState(ids: Seq[VKID]): Unit = {
    used ++= ids
  }
}

import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._
import scala.language.postfixOps

class ReliableUsedActor extends ReliableMessaging {
  this: ReliableUsedBackend =>
  import Used._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: java.net.ConnectException => Restart
    }

  override def persistenceId: String = "used-actor-id"

  override def processCommand: Receive =
  {
    case InsertAndFilter(ids) => deliver(Filtered(insertAndFilter(ids)), sender.path)
  }

  override def preStart = {
    init
    super.preStart
  }
}
