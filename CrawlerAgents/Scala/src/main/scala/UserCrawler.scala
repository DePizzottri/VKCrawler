/*
 * (Simple) Initial users crawler
 */

package VKCrawler.crawlers.users

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBList
import scala.io.Codec
import com.mongodb.util.JSON
import scala.collection.mutable.Queue
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.immutable.HashSet
import scala.collection.immutable.Stream
import scala.annotation._
import scala.concurrent.Await

import scala.concurrent.ExecutionContext.Implicits.global

class User(u:Int) {
  val uid:Int = u
  val friends = {
    Future{
      val friendsURL = s"https://api.vk.com/method/friends.get?user_id=$uid&fields=city";
      val res = scala.io.Source.fromURL(friendsURL)(Codec.UTF8).mkString;
      val resp = JSON.parse(res).asInstanceOf[DBObject]
    
      if (!resp.containsKey("error")) {
        val friends = (
                for (obj <- resp("response").asInstanceOf[BasicDBList]; if obj.asInstanceOf[DBObject].containsKey("city"); if obj.asInstanceOf[DBObject].as[Int]("city") == 148)
              yield obj.asInstanceOf[DBObject].as[Int]("uid"))
    
              //println(s"Done $uid");
          friends.toList
        } else {
          println(s"Empty Done $uid");
          List()
        }
    }
  }
}

object User {
  def apply(u:Int) = new User(u)
}

object Main extends App {
  def insert(uid: Int, friends: List[Int]) = {
    //println(s"Insert $uid")
    users.insert(MongoDBObject("uid" -> uid, "friends" -> friends))
  }
  
  def bfs(start:Int) {
    def aux(cur:List[User], used:Set[Int]):Unit = cur match { 
      case head::tail => {
        val l = Await.result(head.friends, 66 seconds).filter(!used.contains(_))
        insert(head.uid, l)
        aux(tail ::: l.map(User(_)), used + head.uid)
      } 
      case Nil => 
    }
    
    aux(List(User(start)), Set.empty)
  }

  val mongoClient = MongoClient("localhost", 27017)

  val db = mongoClient("VK_test")

  val users = db("users")

  val firstMan = 557323

  bfs(firstMan)
}