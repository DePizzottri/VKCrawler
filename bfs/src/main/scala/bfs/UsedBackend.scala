package vkcrawler.bfs

import vkcrawler.Common._

trait UsedBackend {
  def insertAndFilter(ids:Seq[VKID]): Seq[VKID]
}

trait LocalUsedBackend extends UsedBackend {
  var used = scala.collection.mutable.HashSet.empty[VKID]

  def insertAndFilter(ids:Seq[VKID]) = {
    val ret = ids.filter{x => !used.contains(x)}
    used ++= ids
    ret
  }
}

import redis.clients.jedis._
import com.typesafe.config.ConfigFactory

trait ReliableUsedBackend extends UsedBackend {
  def init: Unit
}

trait JedisUsedBackend extends ReliableUsedBackend {
  this: akka.actor.Actor =>
  val conf = context.system.settings.config
  var jedis:Jedis = null
  val uidsSet = conf.getString("used.redis.setName")

  def init: Unit = {
      jedis = new Jedis(
      conf.getString("used.redis.host"),
      conf.getInt("used.redis.port"),
      conf.getInt("used.redis.timeout")*1000
    )
  }

  override def insertAndFilter(ids:Seq[VKID]) = {
    val pipeline = jedis.pipelined
    ids.foreach { x => pipeline.sadd(uidsSet, x.toString()) }
    val result = pipeline.syncAndReturnAll
    ids.zip(result.toArray).filter { x =>
      x._2.asInstanceOf[Long] == 1
    }.map(_._1)
  }
}
