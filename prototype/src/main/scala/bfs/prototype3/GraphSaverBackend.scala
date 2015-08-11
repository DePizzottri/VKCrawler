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

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._

trait ReliableMongoDBGraphSaverBackend extends ReliableGraphSaverBackend {
  this: akka.actor.Actor =>
  val conf = context.system.settings.config

  var mongoClient: MongoClient = null
  var db:MongoDB = null

  def init(): Unit = {
    mongoClient = MongoClient(conf.getString("MongoDB.host"), conf.getInt("MongoDB.port"))
    db = mongoClient(conf.getString("MongoDB.database"))
  }

  val collection = conf.getString("MongoDB.friends")

  def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
    db(collection).insert(MongoDBObject("id" -> id, "friends" -> ids))
  }
}
