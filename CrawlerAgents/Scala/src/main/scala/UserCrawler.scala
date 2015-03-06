/*
 * (Simple) Initial users crawler
 */

package VKCrawler.crawlers.users

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBList
import scala.io.Codec
import com.mongodb.util.JSON
import scala.collection.mutable.Queue

object Main extends App {
  val mongoClient = MongoClient("localhost", 27017)

  val db = mongoClient("VK_test")

  val users = db("users")

  val firstMan = 557323

  val q = Queue[Int]()

  //start from the first man
  q.enqueue(557323)

  var cnt = 0
  while (q.size != 0) {
    cnt += 1;
    if (cnt % 10 == 0)
      println(s"Queue size: ${q.size}");
    val curId = q.dequeue();

    val friendsURL = s"https://api.vk.com/method/friends.get?user_id=$curId&fields=city";
    try {
      val res = scala.io.Source.fromURL(friendsURL)(Codec.UTF8).mkString;
      val resp = JSON.parse(res).asInstanceOf[DBObject]

      if (!resp.containsKey("error")) {
        val friends = (
          for (obj <- resp("response").asInstanceOf[BasicDBList]; if obj.asInstanceOf[DBObject].containsKey("city"); if obj.asInstanceOf[DBObject].as[Int]("city") == 148)
            yield obj.asInstanceOf[DBObject].as[Int]("uid"))

        val obj = MongoDBObject("uid" -> curId, "friends" -> friends)
        users.insert(obj)

        for (x <- friends) {
          q.enqueue(x);
        }
      }
    } catch {
      case e: Throwable => println("Error " + e.toString())
    }
  }
}