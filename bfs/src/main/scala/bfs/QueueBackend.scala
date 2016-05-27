package vkcrawler.bfs

import vkcrawler.Common._

import java.util.Date

import vkcrawler.DataModel._

import org.joda.time.DateTime

trait QueueBackend {
  def push(ids:Seq[VKID]): Unit
  def pop(`type`:String): Task
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
        bulkInsert.find(MongoDBObject("id" -> id)).upsert().updateOne($set("id" -> id))
      }
      bulkInsert.execute
    }
  }

  val popSize = conf.getInt("queue.popSize")

  def pop(`type`:String): Task = {
    val bulkUpdate = col.initializeUnorderedBulkOperation

    val tryRet = Try{
      val ret = col.find(
        MongoDBObject.empty,
        MongoDBObject("id" -> 1, `type`+".lastUseDate" -> 1)
      )
      .sort(MongoDBObject(`type`+".lastUseDate" -> 1))
      .take(popSize)
      .map { doc =>
        bulkUpdate.find(doc).update($set(`type`+".lastUseDate" -> new Date))
        TaskData(doc.as[VKID]("id"), doc.getAs[MongoDBObject](`type`).map{_.as[DateTime](`type`+".lastUseDate")})
      }.toArray

      bulkUpdate.execute

      ret
    }

    tryRet match {
      case Success(r) => Task(`type`, r)
      case Failure(e) => {
        akka.event.Logging(context.system, this).warning("Failure in queue pop {}", e)
        Task(`type`, Seq())
      }
    }
  }
}

trait LocalQueueBackend extends QueueBackend {
  var queue = scala.collection.mutable.Queue.empty[TaskData]
  def push(ids:Seq[VKID]) = {
    queue ++= ids.map{id => TaskData(id, None)}
  }

  def pop(`type`:String): Task = {
    if(queue.isEmpty)
      Task(`type`, Seq())
    else
      Task(`type`, Seq(queue.dequeue()))
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
