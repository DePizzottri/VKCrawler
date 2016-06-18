package vkcrawler.bfs

import org.elasticsearch.common.settings.Settings
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder

import org.json4s._
import org.json4s.native.JsonMethods._
import org.joda.time.DateTime

import vkcrawler.Common._
import vkcrawler.DataModel._
import org.joda.time.format.ISODateTimeFormat

import scala.concurrent.duration._
import scala.collection._

trait ESQueueBackend extends QueueBackend {
  this: akka.actor.Actor =>
  val conf = context.system.settings.config

  val uri = ElasticsearchClientUri(conf.getString("queue.es.uri"))
  val settings = Settings.settingsBuilder().put("cluster.name", conf.getString("queue.es.clustername")).build()
  val client = ElasticClient.transport(settings, uri)

  val indexName = conf.getString("queue.es.index")
  val typeName = conf.getString("queue.es.type")

  val refreshAfterPop = conf.getBoolean("queue.es.refreshAfterPop")
  val refreshAfterPush = conf.getBoolean("queue.es.refreshAfterPush")

  val pushPerf = new vkcrawler.PerfCounter("PushPerf", context.system, this, 1000)
  val popPerf = new vkcrawler.PerfCounter("PopPerf", context.system, this, 1)

  val timeout = 120.seconds

  def push(ids:Seq[VKID]) = {
    pushPerf.begin
    val now = System.nanoTime
    val queries = ids.map{ Id =>
      update id Id in indexName / typeName docAsUpsert(
        "id" -> Id
      )
    }

    if(queries.size > 0) {
      client.execute {
        bulk(
          queries
        )
      }.await(timeout)

      if(refreshAfterPush)
        client.execute{refreshIndex(indexName)}.await(timeout)
    }
    //val micros = (System.nanoTime - now) / 1000
    //akka.event.Logging(context.system, this).info("Push query time: %d microseconds, size: %d".format(micros, queries.size))
    pushPerf.end
  }

  val taskSize = conf.getInt("queue.taskSize")
  val batchSize = conf.getInt("queue.batchSize")

  var cache = mutable.Map[String, mutable.Queue[Task]]()

  def pop(`type`:String): Task = {
    if(!cache.contains(`type`) || (cache(`type`).length == 0)) {
      cache(`type`) = popAux(`type`)
    }

    if(cache(`type`).length==0)
      Task(`type`, Seq())
    else
      cache(`type`).dequeue
  }

  def popAux(`type`:String): mutable.Queue[Task] = {
    popPerf.begin
    val resp = client.execute {
        search in indexName / typeName limit batchSize sort {
          field sort "lastUseDate." + `type` order SortOrder.ASC nestedPath "lastUseDate" missing "_first"
        } sourceInclude ("id", "lastUseDate." + `type`)
      }.await(timeout)

    val js = parse(resp.original.toString)

    val ids = (js \ "hits" \ "hits" \ "_source" \ "id" \ classOf[JInt]).map(_.toInt)
    val dates = (js \ "hits" \ "hits" \ "_source" \ "lastUseDate" \ `type` \ classOf[JString]).map{x => new DateTime(x.toString)}

    val taskDatas = ids.zipAll(dates.map{x=>Some(x)}, 1, None).map {
      case (id, None) => TaskData(id, None)
      case (id, date) => TaskData(id, date)
    }

    val queries = ids.map { Id =>
      update id Id in indexName / typeName doc {
        "lastUseDate" -> Map(`type` -> new DateTime().toString(ISODateTimeFormat.dateTime().withZoneUTC()))
      }
    }

    if(queries.size > 0) {
      client.execute {
        bulk(
          queries
        )
      }.await(timeout)

      if(refreshAfterPop)
        client.execute{refreshIndex(indexName)}.await(timeout)
    }

    //Task(`type`, taskDatas)
    val tasks = (for(t <- taskDatas.grouped(taskSize)) yield {
      Task(`type`, t)
    })
    val ret = mutable.Queue.empty[Task]
    ret.enqueue(tasks.toSeq:_*)
    popPerf.end
    ret
  }
}

trait ReliableESQueueBackend extends ReliableQueueBackend with ESQueueBackend {
  this: akka.actor.Actor =>

  def recoverQueue: Unit = {
  }
}
