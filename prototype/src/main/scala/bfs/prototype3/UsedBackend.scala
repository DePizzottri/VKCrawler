package vkcrawler.bfs.prototype3

import Common._

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

trait RedisUsedBackend extends UsedBackend {
  def insertAndFilter(ids:Seq[VKID]) = {
    throw new Exception("Not implemented")
  }
}