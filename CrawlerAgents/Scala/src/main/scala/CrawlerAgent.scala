package com.vkcrawler.crawler.basic
=======
/*
 * Friends list crawler 
 */

package VKCrawler.crawlers.basic
>>>>>>> origin/master

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBList
import scala.io.Codec
import com.mongodb.util.JSON

object Main extends App {
  val mongoClient = MongoClient("localhost", 27017)

  val db = mongoClient("VK_test")

  val tasks = db("tasks")

  while (true) {
    val optTask = tasks.findAndModify(MongoDBObject(), MongoDBObject("usecount" -> 1), $inc("usecount" -> 1))

    if (optTask.isEmpty)
      throw new RuntimeException("NoData")

    val task = optTask.get

    println(task("_id"))

    val URLs =
      for (URI <- task("data").asInstanceOf[BasicDBList])
        yield (URI.asInstanceOf[DBObject].as[String]("URL"))

    val friends_list = db("friends_list")
    for (URL <- URLs) {
      println(URL)
      try
      {
        val res = scala.io.Source.fromURL(URL)(Codec.UTF8).mkString

        val resp = JSON.parse(res).asInstanceOf[DBObject]
        if (!resp.containsKey("error")) {
          val id = URL.split("user_id=").toList.tail.head.toInt
  
          val friends = resp.as[List[Int]]("response")
          val obj = MongoDBObject("uid" -> id, "friends" -> friends)
          friends_list.insert(obj)
        }
      }
      catch
      {
        case _:Throwable => Unit
      }        
    }
  }

}