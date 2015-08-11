package vkcrawler.bfs.prototype3.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._

class GraphSaverMongoDBIntegrationSpec(_system: ActorSystem) extends BFSTestSpec(_system) {
  def this() = this(ActorSystem("GraphSaverMongoDBIntegrationSpecSystem"))

  import vkcrawler.bfs.prototype3._

  "ReliableGraphActor " must {
    "Save friends list" in {
      import Common._

      class GraphSaverMongoDBActor extends ReliablGraphActor with ReliableMongoDBGraphSaverBackend

      import ReliableMessaging._
      val graph = system.actorOf(Props(new GraphSaverMongoDBActor))
      val friends = BFS.Friends(1, Seq[VKID](1, 2, 3))

      graph ! Envelop(1, friends)

      expectMsg(Confirm(1))

      val conf = system.settings.config

      val mongoClient = MongoClient(conf.getString("MongoDB.host"), conf.getInt("MongoDB.port"))
      val db = mongoClient(conf.getString("MongoDB.database"))
      val col = db(conf.getString("MongoDB.friends"))

      val data = col.findOne(MongoDBObject("1" -> Seq[VKID](1, 2, 3)))
      data should not be equal (None)

      db.dropDatabase
    }
  }
}
