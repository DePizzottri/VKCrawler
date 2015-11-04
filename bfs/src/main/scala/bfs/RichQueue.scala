package vkcrawler.bfs

import akka.actor._
import akka.persistence._

import vkcrawler.Common._
import org.joda.time.DateTime
import scala.util.Random

/*
  Queue actor
  Process guaranee + (resend on failure)
  Delivery guarantee + (at-least-once)
  No snapshots
*/

import vkcrawler.DataModel._

object RichQueue {
  case class Push(ids:Seq[VKID])
  case class Pop(types: Seq[String])
  case class Item(task:Task)
  //case class Empty()

  class Evt()
  case class Poped(`type`:String) extends Evt
  case class DataFromBackend(`type`:String, tasks:Seq[Task]) extends Evt
  //case class SaveSnapshot()
  case class DeliverItem(task:Task, path:ActorPath)
}

object ReliableRichQueueActor {
  import scala.collection.mutable.Map
  import scala.collection.mutable.Queue
  val Queues = Map
}

abstract class ReliableRichQueueActor extends ReliableMessaging with Stash {
  import RichQueue._

  override def persistenceId: String = "rich-queue-actor-id"

  import scala.collection.mutable.Map
  import scala.collection.mutable.Queue
  type Queues = Map[String, Queue[Task]]
  var queues = ReliableRichQueueActor.Queues.empty[String, Queue[Task]]

  override def storeSnapshot(childStates: List[Any]):Unit = {
    //println(s"Queue snapshot store: ${queues}");
    super.storeSnapshot(queues :: childStates)
  }

  override def restoreFromSnapshot(states:List[Any]): Unit = {
    //println(s"Queue snapshot offer: ${states.head}");
    queues = states.head.asInstanceOf[Queues]
    super.restoreFromSnapshot(states.tail)
  }

  def createBackend: RichQueueBackendActor
  val backend = context.system.actorOf(Props(createBackend))

  val demandThreshold = 10

  def updateState(evt:Evt) = evt match {
    case DataFromBackend(tp, tasks) => {
      queues.getOrElseUpdate(tp, Queue()) ++= tasks
    }
    case Poped(tp) => {
      queues(tp).dequeue
    }
  }

  override def processRecover: Receive = {
    case evt: Evt => updateState(evt)
    case msg:RecoveryCompleted => Unit
  }

  override def processCommand: Receive =
  {
    //case SaveSnapshot() => saveSnapshot(queues)
    case RichQueueBackendProtocol.QueueData(tp, tasks) => persist(DataFromBackend(tp, tasks)) { evt =>
      updateState(evt)
      unstashAll
    }
    case Push(ids) => {
      backend ! RichQueueBackendProtocol.Push(ids)
    }
    case Pop(types) => {
      //  println("===================")
      //  println(types)
      //  println(queues)
      //recalc demand
      val thresTypes = queues.filter { case (k, v) =>
        v.size < demandThreshold
      } map { case (k, v) =>
        k
      }

      val demandedTypes = thresTypes ++ types.filter{x => !queues.keySet.contains(x)}
      //println(demandedTypes)

      if(demandedTypes.size > 0) {
        backend ! RichQueueBackendProtocol.Demand(demandedTypes.toSeq)
      }

      //find tasks with ready types
      val taskCandidates = queues.filter { case (k, v) =>
        types.contains(k) && v.size > 0
      }

      //if all types demanded
      if(taskCandidates.size == 0) {
        stash
      }
      else {
        val task = taskCandidates.keys.toVector(Random.nextInt(taskCandidates.size))
        persist(Poped(task)) { evt =>
          val item = taskCandidates(evt.`type`).dequeue
          //println(s"Deliver $item")
          //deliver(Item(item), sender.path)
          //until 2.4
          self ! DeliverItem(item, sender.path)
        }
      }
    }

    case DeliverItem(item, path) => {
      deliver(Item(item), path)
    }
  }
}

object RichQueueBackendProtocol {
  import scala.collection.immutable.Map
  case class QueueData(`type`:String, tasks:Seq[Task])
  case class Push(ids:Seq[VKID])
  case class Demand(types:Seq[String])
}

class RichQueueBackendActor extends Actor {
  this: RichQueueBackend =>
  import RichQueueBackendProtocol._

  val taskSize = context.system.settings.config.getInt("queue.taskSize")
  val batchSize = context.system.settings.config.getInt("queue.batchSize")

  override def receive = {
    case Push(ids) => {
      push(ids)
    }
    case Demand(types) => {
      popMany(types, taskSize, batchSize) foreach { case (t, s) =>
        sender ! QueueData(t, s)
      }
    }
  }
}
