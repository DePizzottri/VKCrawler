package vkcrawler.bfs

import vkcrawler.Common._

import java.util.Date

trait QueueBackend {
  def push(ids:Seq[VKID]): Unit
  def popMany(): Seq[VKID]
}

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._
import scala.util._

trait MongoQueueBackend extends QueueBackend {
  this: akka.actor.Actor =>
  val conf = context.system.settings.config

  var mongoClient = MongoClient(conf.getString("queue.mongodb.host"), conf.getInt("queue.mongodb.port"))
  var db = mongoClient(conf.getString("queue.mongodb.database"))
  var col = db(conf.getString("queue.mongodb.queue"))

  def push(ids:Seq[VKID]) = {
    if(!ids.isEmpty) {
      val bulkInsert = col.initializeUnorderedBulkOperation
      ids.foreach { id =>
        bulkInsert.insert(MongoDBObject("id" -> id))
      }
      bulkInsert.execute
    }
  }


  def popMany(): Seq[VKID] = {
    // col.findOne() match {
    //   case Some(doc:DBObject) => {
    //     col.remove(doc)
    //     doc.getAs[VKID]("id") match {
    //       case Some(id) => Seq(id)
    //       case None => Seq()
    //     }
    //   }
    //   case None => Seq()
    // }
    val bulkUpdate = col.initializeUnorderedBulkOperation

    val tryRet = Try{
      val ret = col.find(
        MongoDBObject.empty,
        MongoDBObject("id" -> 1)
      )
      .sort(MongoDBObject("lastUseDate" -> 1))
      .take(50)
      .flatMap { doc =>
        bulkUpdate.find(doc).update($set("lastUseDate" -> new Date))
        doc.getAs[VKID]("id")
      }.toArray

      bulkUpdate.execute

      ret
    }

    tryRet match {
      case Success(r) => r
      case Failure(e) => Seq()
    }
  }
}

trait LocalQueueBackend extends QueueBackend {
  var queue = scala.collection.mutable.Queue.empty[VKID]
  def push(ids:Seq[VKID]) = {
    queue ++= ids
  }

  def popMany(): Seq[VKID] = {
    if(queue.isEmpty)
      Seq.empty[VKID]
    else
      Seq(queue.dequeue())
  }
}

trait ReliableQueueBackend extends QueueBackend {
  def recoverQueue: Unit
}

trait ReliableMongoQueueBackend extends ReliableQueueBackend with MongoQueueBackend {
  this: akka.actor.Actor =>

  def recoverQueue: Unit = {
    mongoClient = MongoClient(conf.getString("queue.mongodb.host"), conf.getInt("queue.mongodb.port"))
    db = mongoClient(conf.getString("queue.mongodb.database"))
    col = db(conf.getString("queue.mongodb.queue"))
  }
}

trait ReliableLocalQueueBackend extends ReliableQueueBackend with LocalQueueBackend {
  def recoverQueue: Unit = {
    //no reliability?
  }
}
