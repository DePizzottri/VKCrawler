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
  case class Empty()
}

class ReliableRichQueueActor extends ReliableMessaging {
  import RichQueue._

  override def persistenceId: String = "rich-queue-actor-id"

  import scala.collection.mutable.Map
  import scala.collection.mutable.Queue
  var queues = Map.empty[String, Queue[Task]]

  class BackendActor extends RichQueueBackendActor with LocalRichQueueBackend
  val backend = context.system.actorOf(Props(new BackendActor))

  val demandThreshold = 10

  override def processCommand: Receive =
  {
    case RichQueueBackendProtocol.QueueData(tp, tasks) => {
      queues.getOrElseUpdate(tp, Queue()) ++= tasks
    }
    case Push(ids) => {
      backend ! RichQueueBackendProtocol.Push(ids)
    }
    case Pop(types) => {
      //find tasks with types
      val taskCandidates = queues.filter{ case (k, v) =>
        types.contains(k) && v.size > 0
      }

      if (taskCandidates.size > 0) {
        val task = taskCandidates.keys.toVector(Random.nextInt(taskCandidates.size))
        val item = taskCandidates(task).dequeue
        deliver(Item(item), sender.path)
      } else {
        deliver(Empty, sender.path)
      }

      //recalc demand
      val thresTypes = queues.filter{ case (k, v) =>
        v.size < demandThreshold
      } map { case (k, v) =>
        k
      }

      val demandedTypes = thresTypes ++ types.filter{x => !queues.keySet.contains(x)}

      if(demandedTypes.size > 0) {
        backend ! RichQueueBackendProtocol.Demand(demandedTypes.toSeq)
      }
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

  val conf = context.system.settings.config

  val taskSize = conf.getInt("queue.taskSize")
  val batchSize = conf.getInt("queue.batchSize")

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
