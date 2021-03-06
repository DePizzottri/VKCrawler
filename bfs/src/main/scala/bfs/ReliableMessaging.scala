package vkcrawler.bfs

import akka.actor.{Actor, ActorPath}
import scala.concurrent.duration._
//import kamon.Kamon

object ReliableMessaging {
  case class Envelop(deliveryId:Long, msg:Any)
  case class Confirm(deliveryId:Long)

  //case class SaveDeliverySnapshot()

  sealed trait ALODEvt
  case class MsgSent(msg:Any, destination: ActorPath) extends ALODEvt
  case class MsgConfirmed(deliveryId:Long) extends ALODEvt
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

trait ReliableMessaging extends PersistentActor with AtLeastOnceDelivery with EnvelopReceive with HierarchicalSnapshotStore {
  import ReliableMessaging._

  private[ReliableMessaging] def processConfirm: Receive = {
    case Confirm(deliveriId) => persist(MsgConfirmed(deliveriId)){updateStateMsg}
    //case SaveDeliverySnapshot() => saveSnapshot(getDeliverySnapshot)
    case SaveSnapshotSuccess(metadata) => Unit
  }

  override def receiveCommand: Receive = withPostConfirmation(processConfirm orElse processCommand orElse snapshotReceiveing)

  override def receiveRecover: Receive = withPostConfirmation(recover orElse processRecover orElse snapshotRecover)

  def processCommand: Receive

  override def storeSnapshot(childStates: List[Any]):Unit = {
    //println(s"Delvery snapshot store: ${getDeliverySnapshot}");
    super.storeSnapshot(getDeliverySnapshot :: childStates)
  }

  override def restoreFromSnapshot(states:List[Any]): Unit = {
    //println(s"Delvery snapshot offer: ${states.head}");
    setDeliverySnapshot(states.head.asInstanceOf[AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot])
    super.restoreFromSnapshot(states.tail)
  }

  def recover: Receive = {
    case evt: ALODEvt => updateStateMsg(evt)
    // case SnapshotOffer(_, snapshot: AtLeastOnceDelivery.AtLeastOnceDeliverySnapshot) => {
    //   println(s"A $snapshot"); setDeliverySnapshot(snapshot)
    // }
  }

  def deliver(msg: Any, dest: ActorPath) = {
    persist(MsgSent(msg, dest))(updateStateMsg)
  }

  def updateStateMsg(evt: ALODEvt): Unit = evt match {
    case MsgSent(msg, dest) =>
      deliver(dest)(deliveryId => Envelop(deliveryId, msg))

    case MsgConfirmed(deliveryId) => confirmDelivery(deliveryId)
  }

  def processRecover: Receive = {
    case msg:RecoveryCompleted => Unit
  }

  val config = context.system.settings.config

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  private[ReliableMessaging] def loadDuration(path: String) =  {
    val dur = config.getDuration(path)
    FiniteDuration(dur.toMillis, "ms")
  }

  context.system.scheduler.schedule(
    loadDuration("reliable-messaging.unconfirmed-interval"),
    loadDuration("reliable-messaging.unconfirmed-interval")) {
    //Kamon.metrics.histogram(s"${self.path.name}/unconfirmed-messages").record(numberOfUnconfirmed)
  }

  context.system.scheduler.schedule(
    loadDuration("reliable-messaging.snapshot-interval"),
    loadDuration("reliable-messaging.snapshot-interval")) {
    self ! HierarchicalSnapshotStore.SaveSnapshot//SaveDeliverySnapshot()
  }
}

object HierarchicalSnapshotStore {
  final case object SaveSnapshot
}

trait HierarchicalSnapshotStore {
  this: PersistentActor =>

  def snapshotReceiveing: Receive = {
    case HierarchicalSnapshotStore.SaveSnapshot => {
      storeSnapshot(Nil)
    }
  }

  def snapshotRecover: Receive = {
    case SnapshotOffer(_, snapshot: List[Any]) => restoreFromSnapshot(snapshot)
  }

  def storeSnapshot(childStates: List[Any]): Unit = {
    saveSnapshot(childStates.reverse)
  }

  def restoreFromSnapshot(states:List[Any]): Unit = {
    assert(states == Nil)
  }
}
