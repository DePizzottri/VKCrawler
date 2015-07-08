package vkcrawler.bfs.prototype3

import akka.actor.{Actor, ActorPath}

object ReliableMessaging {
  case class Envelop(deliveryId:Long, msg:Any)

  sealed trait ALODEvt
  case class MsgSent(msg:Any, destination: ActorPath) extends ALODEvt
  case class Confirm(deliveryId:Long) extends ALODEvt
}

trait EnvelopReceive {
  this: Actor => 
  import ReliableMessaging._

  def openEnvelopAndPreConfirm: PartialFunction[Any, Any] = {
    case Envelop(id, msg) => sender ! Confirm(id); msg
    case msg => msg
  }

  def withPostConfirmation(r: Receive): Receive = {
    case Envelop(id, msg) => r(msg); sender ! Confirm(id)
    case msg => r(msg) 
  }
}

import akka.persistence._

trait ReliableMessaging extends PersistentActor with AtLeastOnceDelivery with EnvelopReceive {
  import ReliableMessaging._

  override def receiveCommand: Receive = withPostConfirmation(processCommand)
  
  override def receiveRecover: Receive = withPostConfirmation(recover orElse processRecover)

  def processCommand: Receive

  def processRecover: Receive = PartialFunction.empty

  def recover: Receive = {
    case evt: ALODEvt => updateStateMsg(evt)
  }

  def deliver(msg: Any, dest: ActorPath) = {
    persist(MsgSent(msg, dest))(updateStateMsg)
  }

  def updateStateMsg(evt: ALODEvt): Unit = evt match {
    case MsgSent(msg, dest) =>
      deliver(dest, deliveryId => Envelop(deliveryId, msg))
 
    case Confirm(deliveryId) => confirmDelivery(deliveryId)
  }
}

/*
trait ResendOnError {
  this: akka.actor.Actor =>
  override def preRestart(reason: Throwable, message: Option[Any]) {
    super.preRestart(reason, message)
    message match {
      case Some(m) => self ! m
      case None => None
    }
  }
}
*/