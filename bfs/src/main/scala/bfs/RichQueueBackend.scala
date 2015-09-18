package vkcrawler.bfs

import vkcrawler.Common._
import vkcrawler.DataModel._
import scala.collection.immutable.Map

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
    val ret = (for (j <- 1 to count) yield {
      val ids = (
        for (i <- 1 to taskSize if !queue.isEmpty) yield {queue.dequeue}
      )
      Task(`type`, ids)
    })

    ret.filter{t => t.ids.size != 0}
  }
}
