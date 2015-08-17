package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import scala.util.Random

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._

object GraphSaverMongoDBIntegrationSpec {
  private def getRandomCollection = Random.alphanumeric.take(5).mkString

  def config = ConfigFactory.parseString(
    """
    graph {
      mongodb {
        host = 192.168.1.9
        port = 27017
        database = vkcrawler_graph_test
        friends = friends
      }
    }
    """.stripMargin
  )
}

class GraphSaverMongoDBIntegrationSpec(_system: ActorSystem) extends BFSTestSpec(_system) {
  def this() = this(
    ActorSystem(
      "GraphSaverMongoDBIntegrationSpecSystem",
      GraphSaverMongoDBIntegrationSpec.config.withFallback(PersistanceSpecConfiguration.config)
      )
    )

  override def afterAll {
    system.shutdown()
  }


  import vkcrawler.bfs.prototype3._

  "GraphSaverMongoDBActor " must {
    "Save friends list" in {
      import Common._

      class GraphSaverMongoDBActor extends ReliableGraphActor with ReliableMongoDBGraphSaverBackend

      import ReliableMessaging._
      val graph = system.actorOf(Props(new GraphSaverMongoDBActor))
      val friends = BFS.Friends(1, Seq[VKID](1, 2, 3))

      graph ! Envelop(1, friends)

      expectMsg(10.seconds, Confirm(1))

      val conf = system.settings.config

      val mongoClient = MongoClient(conf.getString("graph.mongodb.host"), conf.getInt("graph.mongodb.port"))
      val db = mongoClient(conf.getString("graph.mongodb.database"))
      val col = db(conf.getString("graph.mongodb.friends"))

      val data = col.findOne(MongoDBObject("1" -> Seq[VKID](1, 2, 3)))
      data should not be equal (None)

      db.dropDatabase
    }
  }
}
