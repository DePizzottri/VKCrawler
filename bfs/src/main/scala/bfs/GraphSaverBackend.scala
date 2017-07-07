package vkcrawler.bfs

import vkcrawler.Common._

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
    mongoClient = MongoClient(conf.getString("graph.mongodb.host"), conf.getInt("graph.mongodb.port"))
    db = mongoClient(conf.getString("graph.mongodb.database"))
  }

  val collection = conf.getString("graph.mongodb.friends")
  val isUpsert = conf.getBoolean("graph.upsert")

  def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
    if(!isUpsert)
      db(collection).insert(MongoDBObject("id" -> id, "friends" -> ids))
    else {
      val query = MongoDBObject("id" -> id)
      val update = $set("friends" -> ids)
      val res = db(collection).update(query, update, upsert=true)
    }
  }
}

import org.anormcypher._
import play.api.libs.ws._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait ReliableNeo4jGraphSaverBackend extends ReliableGraphSaverBackend {
  this: akka.actor.Actor =>
  val conf = context.system.settings.config

  var wsclient: ning.NingWSClient = null

  var connection: Neo4jConnection = null

  def init(): Unit = {
    implicit val materializer = ActorMaterializer()
    wsclient = ning.NingWSClient()
    implicit val wsc = wsclient
    connection = Neo4jREST(conf.getString("graph.neo4j.host"), conf.getInt("graph.neo4j.port"))
  }

  def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
    implicit val ec = scala.concurrent.ExecutionContext.global
    implicit val wsc = wsclient
    implicit val con = connection

    Cypher(
      s"""
WITH ${ids.mkString("[", ",", "]")} as ids
UNWIND ids as u2id
MERGE (u1:User {Id:${id}})
MERGE (u2:User {Id:u2id})
CREATE UNIQUE p = (u1) - [:FRIEND] -> (u2)
"""
).execute()
  }
}
