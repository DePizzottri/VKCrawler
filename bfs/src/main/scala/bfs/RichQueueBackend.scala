package vkcrawler.bfs

import vkcrawler.Common._
import vkcrawler.DataModel._
import scala.collection.immutable.Map

import org.joda.time.DateTime

trait RichQueueBackend {
  def push(ids:Seq[VKID]): Unit
  def pop(`type`:String, taskSize:Int, count:Int): Seq[Task]
  def popMany(types:Seq[String], taskSize:Int, count:Int):Map[String, Seq[Task]] = {
    (for (t <- types) yield {
      (t, pop(t, taskSize, count))
    }).toMap
  }
}

trait LocalRichQueueBackend extends RichQueueBackend {
  //one queue for all task types
  var queue = scala.collection.mutable.Queue.empty[VKID]

  def push(ids:Seq[VKID]): Unit = {
    queue ++= ids
  }

  def pop(`type`:String, taskSize:Int, count:Int): Seq[Task] = {
    //println(s"Pop: $queue $taskSize $count")
    val ret = (for (j <- 1 to count) yield {
      val datas = (
        for (i <- 1 to taskSize if !queue.isEmpty) yield {
          TaskData(queue.dequeue, None)
        }
      )
      Task(`type`, datas)
    })

    ret.filter{t => t.data.size != 0}.foreach{t => queue ++= t.data.map{_.id}}

    ret.filter{t => t.data.size != 0}
  }
}

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._
import scala.util._
import java.util.Date

trait MongoRichQueueBackend extends RichQueueBackend {
  this: akka.actor.Actor =>

  import com.mongodb.casbah.commons.conversions.scala._
  RegisterJodaTimeConversionHelpers()

  val conf = context.system.settings.config

  var mongoClient = MongoClient(conf.getString("queue.mongodb.host"), conf.getInt("queue.mongodb.port"))
  var db = mongoClient(conf.getString("queue.mongodb.database"))
  var col = db(conf.getString("queue.mongodb.queue"))

  val log = akka.event.Logging(context.system, this)

  def push(ids:Seq[VKID]) = {
    if(!ids.isEmpty) {
      val bulkInsert = col.initializeUnorderedBulkOperation
      ids.foreach { id =>
        bulkInsert.find(MongoDBObject("id" -> id)).upsert().updateOne($set("id" -> id))
      }
      bulkInsert.execute
    }
  }

  def pop(`type`:String, taskSize:Int, count:Int): Seq[Task] = {
    val bulkUpdate = col.initializeUnorderedBulkOperation

    val tryRet = Try {
      val ret = col.find(
        MongoDBObject.empty,
        MongoDBObject("id" -> 1, `type`+".lastUseDate" -> 1)
      )
      .sort(MongoDBObject(`type`+".lastUseDate"-> 1))
      .take(taskSize*count)
      .map { doc =>
        bulkUpdate.find(doc).update($set(`type`+".lastUseDate" -> new Date))
        TaskData(doc.as[VKID]("id"), doc.getAs[MongoDBObject](`type`).map{_.as[DateTime]("lastUseDate")})
      }.toArray

      bulkUpdate.execute

      ret
    }


    tryRet match {
      case Success(r) =>  {
        (for(t <- r.grouped(taskSize)) yield {
          Task(`type`, t)
        })
        .toVector
      }
      case Failure(e) => {
        log.warning("Failure in queue pop {}", e)
        Seq()
      }
    }
  }
}
