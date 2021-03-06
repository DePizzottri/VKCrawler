package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import akka.testkit.TestProbe
import scala.util.Random
import com.typesafe.config.ConfigFactory

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel
import com.rabbitmq.client.QueueingConsumer

object WholeSpec {
  private def getRandomCollection = Random.alphanumeric.take(5).mkString

  def config = ConfigFactory.parseString(
    """
    """.stripMargin
  )
}

class WholeSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem(
    "WholeIntegrationSpecSystem",
    WholeSpec.config.withFallback(RichQueueSpec.config.withFallback(PersistanceSpecConfiguration.config))))

  import vkcrawler.bfs._

  "All simple BFS actors " must {
    "process simple graph " in {
      import vkcrawler.Common._

      class DummyGraphActor extends GraphActor with GraphSaverBackend {
        var fr = Map.empty[VKID, Seq[VKID]]

        def saveFriends(id:VKID, ids:Seq[VKID]): Unit = {
          fr += (id -> ids)
        }

        override def receive: Receive = super.receive orElse {
          case "get" => sender ! fr
        }
      }

      val p = TestProbe()

      class LocalUsedActor extends UsedActor with LocalUsedBackend
      class TestRichQueueActor extends ReliableRichQueueActor {
        class LocalBackendActor extends RichQueueBackendActor with LocalRichQueueBackend
        override def persistenceId = "test-rich-queue-id-whole"
        override def createBackend = new LocalBackendActor
        override val demandThreshold = 2
      }
      val queue = system.actorOf(Props(new TestRichQueueActor), "QueueActor")

      val bfspath = ActorPath.fromString(system.toString+"/user/BFSActor")

      class DummyExchange extends ExchangeActor(
        bfspath,
        queue.path
      ) with DummyExchangeBackend
      val exchange = system.actorOf(Props(new DummyExchange), "ExchangeActor")

      val graph = system.actorOf(Props(new DummyGraphActor), "GraphActor")
      val used = system.actorOf(Props(new LocalUsedActor), "UsedActor")

      val bfs = system.actorOf(Props(new BFSActor(graph.path, used.path, exchange.path)), "BFSActor")

      queue ! Queue.Push(Seq(1l))

      val g = Map[VKID, Seq[VKID]](1l -> Seq[VKID](2l, 3l), 2l->Seq[VKID](4l), 4l->Seq(5l))

      class Crawler extends Actor {
        import vkcrawler.Common._
        import ReliableMessaging._

        import scala.concurrent.ExecutionContext.Implicits.global
        system.scheduler.schedule(20.milliseconds, 20.milliseconds, self, "Run")

        def receive = {
          case "Run" => {
            queue ! Queue.Pop("type1")
          }
          case Envelop(deliveryId, RichQueue.Item(task)) => {
            task.data.foreach { taskData =>
              exchange ! BFS.Friends(taskData.id, g.getOrElse(taskData.id, Seq()))
            }
            sender ! Confirm(deliveryId)
          }
        }
      }

      val crawler = system.actorOf(Props(new Crawler), "CrawlerActor")

      expectNoMsg(5.seconds)

      graph ! "get"

      expectMsg(g + (3l->Seq()) + (5l->Seq()))
    }
  }
}
