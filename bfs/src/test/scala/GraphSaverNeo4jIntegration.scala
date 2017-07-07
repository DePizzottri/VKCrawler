package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import scala.util.Random

import org.anormcypher._
import play.api.libs.ws._
import akka.stream.ActorMaterializer

object GraphSaverNeo4jIntegrationSpec {
  private def getRandomCollection = Random.alphanumeric.take(5).mkString

  def config = ConfigFactory.parseString(
    """
    graph {
      neo4j {
        host = localhost
        port = 7474
      }

    }
    """.stripMargin
  )
}

class GraphSaverNeo4jIntegrationSpec(_system: ActorSystem) extends BFSTestSpec(_system) {
  def this() = this(
    ActorSystem(
      "GraphSaverNeo4jIntegrationSpecSystem",
      GraphSaverNeo4jIntegrationSpec.config.withFallback(PersistanceSpecConfiguration.config)
      )
    )

  override def afterAll {
    system.shutdown()
  }


  import vkcrawler.bfs._

  "GraphSaverNeo4jActor " must {
    "Save friends list" in {
      import vkcrawler.Common._

      class GraphSaverNeo4jActor extends ReliableGraphActor with ReliableNeo4jGraphSaverBackend

      import ReliableMessaging._
      val graph = system.actorOf(Props(new GraphSaverNeo4jActor))
      val friends = BFS.Friends(1, Seq[VKID](1, 2, 3))

      graph ! Envelop(1, friends)

      expectMsg(10.seconds, Confirm(1))

      val conf = system.settings.config

      implicit val materializer = ActorMaterializer()
      implicit val wsclient = ning.NingWSClient()
      implicit val connection = Neo4jREST(conf.getString("graph.neo4j.host"), conf.getInt("graph.neo4j.port"))
      implicit val ec = scala.concurrent.ExecutionContext.global

      val edgesQuery = Cypher("MATCH p = (:User) - [:FRIEND] -> (:User) RETURN count(p) as cnt");

      val edges = edgesQuery.apply().map{ row=>
        row[Long]("cnt")
      }.toList

      edges should equal (List(3L))

      val result = Cypher("""
        WITH [1,2,3] as ids
        UNWIND ids as u2id
        MATCH (u:User {Id:u2id}) DETACH DELETE u;
        """).execute()
    }
  }
}
