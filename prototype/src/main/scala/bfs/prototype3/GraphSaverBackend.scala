package vkcrawler.bfs.prototype3

import Common._

import java.io.PrintWriter
import java.io.File


trait GraphSaverBackend {
  def saveFriends(id:VKID, ids:Seq[VKID]): Unit
}

trait FileGraphSaverBackend extends GraphSaverBackend {
  val fileName:String

  val pw = new PrintWriter(new File(fileName))

  def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
    pw.write(s"$id -> $ids\n")
  }
}

trait ReliableGraphSaverBackend extends GraphSaverBackend {
  def init(): Unit
  def saveFriends(id:VKID, ids:Seq[VKID]): Unit
}

trait ReliableFileGraphSaverBackend extends ReliableGraphSaverBackend {
  var pw:PrintWriter = null
  val fileName:String

  def init(): Unit = {
    pw = new PrintWriter(new File(fileName))
  }

  def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
    pw.write(s"$id -> $ids\n")
  }
}

trait ReliableMongoDBGraphSaverBackend extends ReliableGraphSaverBackend {
  def init(): Unit = {
    throw new Exception("Not implemented")
  }

  def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
    throw new Exception("Not implemented")
  }
}
