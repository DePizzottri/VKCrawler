package vkcrawler.bfs.prototype3

import Common._

trait QueueBackend {
  def push(ids:Seq[VKID]): Unit
  def popMany(): Seq[VKID]
}

trait MongoQueueBackend extends QueueBackend {
  def push(ids:Seq[VKID]) = {
    throw new Exception("Not implemented")
    Unit
  }

  def popMany(): Seq[VKID] = {
    throw new Exception("Not implemented")
    Seq()
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
  def recoverQueue: Unit = {
    //reconnect
  }
}

trait RelibleLocalQueueBackend extends ReliableQueueBackend with LocalQueueBackend {
  def recoverQueue: Unit = {
    //no reliability?
  }
}