package vkcrawler.bfs

import vkcrawler.Common._

import java.util.Date

import vkcrawler.DataModel._

import org.joda.time.DateTime

import scala.collection._

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

  var mongoClient = MongoClient(MongoClientURI(conf.getString("queue.mongodb.uri")))
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

  val taskSize = conf.getInt("queue.taskSize")
  val batchSize = conf.getInt("queue.batchSize")

  var cache = mutable.Map[String, mutable.Queue[Task]]()

  def pop(`type`:String): Task = {
    if(!cache.contains(`type`) || (cache(`type`).length == 0)) {
      cache(`type`) = popAux(`type`)
    }

    //println(`type`)
    //println(cache.size)
    //println(cache(`type`).length)

    if(cache(`type`).length==0)
      Task(`type`, Seq())
    else
      cache(`type`).dequeue
  }

  def popAux(`type`:String): mutable.Queue[Task] = {
    val now = System.nanoTime
    val bulkUpdate = col.initializeUnorderedBulkOperation

    val tryRet = Try{
      val ret = col.find(
        MongoDBObject.empty,
        MongoDBObject("id" -> 1, `type`+".lastUseDate" -> 1)
      )
      .sort(MongoDBObject(`type`+".lastUseDate" -> 1))
      .take(batchSize + scala.util.Random.nextInt(batchSize))
      .map { doc =>
        bulkUpdate.find(doc).update($set(`type`+".lastUseDate" -> new Date))
        TaskData(doc.as[VKID]("id"), doc.getAs[MongoDBObject](`type`).map{x => new DateTime(x.as[Date]("lastUseDate"))})
      }.toArray

      bulkUpdate.execute

      ret
    }

    val micros = (System.nanoTime - now) / 1000
    akka.event.Logging(context.system, this).info("Mongo query time: %d microseconds".format(micros))

    tryRet match {
      case Success(r) => {
        val tasks = (for(t <- r.grouped(taskSize)) yield {
          Task(`type`, t)
        })
        val ret = mutable.Queue.empty[Task]
        ret.enqueue(tasks.toSeq:_*)
        ret
      }
      case Failure(e) => {
        akka.event.Logging(context.system, this).warning("Failure in queue pop {}", e)
        mutable.Queue.empty
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
    mongoClient = MongoClient(MongoClientURI(conf.getString("queue.mongodb.uri")))
    db = mongoClient(conf.getString("queue.mongodb.database"))
    col = db(conf.getString("queue.mongodb.queue"))
  }
}

trait ReliableLocalQueueBackend extends ReliableQueueBackend with LocalQueueBackend {
  def recoverQueue: Unit = {
    //no reliability?
  }
}
