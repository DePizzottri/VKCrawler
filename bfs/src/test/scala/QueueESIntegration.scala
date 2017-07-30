package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random

object QueueESIntegration {
  private def getRandomCollection = Random.alphanumeric.take(5).mkString

  val popSize = 2

  def config = ConfigFactory.parseString(
    s"""
    queue {
      es {
        uri = "elasticsearch://localhost:9300"
        clustername = "meowes"
        index = "queue_test"
        type = "queue"
        refreshAfterPop = true
        refreshAfterPush = true
      }
      taskSize = $popSize
      batchSize = 10
    }
    """.stripMargin
  )
}

class QueueESIntegration(_system: ActorSystem) extends BFSTestSpec(_system) {
  import org.elasticsearch.common.settings.Settings
  import com.sksamuel.elastic4s._
  import com.sksamuel.elastic4s.ElasticDsl._
  import com.sksamuel.elastic4s.mappings.FieldType._

  def this() = this(
    ActorSystem(
      "QueueESIntegrationSpecSystem",
      QueueESIntegration.config.withFallback(PersistanceSpecConfiguration.config)
      )
    )


  override def beforeAll {
    val conf = system.settings.config

    val indexName = conf.getString("queue.es.index")
    val typeName = conf.getString("queue.es.type")

    val uri = ElasticsearchClientUri(conf.getString("queue.es.uri"))
    val settings = Settings.settingsBuilder().put("cluster.name", conf.getString("queue.es.clustername")).build()
    val client = ElasticClient.transport(settings, uri)

    client.execute {
      create index indexName
    }.await
  }

  override def afterAll {
    //cleanup
    val conf = system.settings.config

    val indexName = conf.getString("queue.es.index")
    val typeName = conf.getString("queue.es.type")

    val uri = ElasticsearchClientUri(conf.getString("queue.es.uri"))
    val settings = Settings.settingsBuilder().put("cluster.name", conf.getString("queue.es.clustername")).build()
    val client = ElasticClient.transport(settings, uri)

    client.execute { deleteIndex(indexName) }.await
  }

  import vkcrawler.bfs._
  import vkcrawler.DataModel._

  class QueueESDBPopActor extends QueuePopActor with ESQueueBackend
  class QueueESDBPushActor extends QueuePushActor with ESQueueBackend

  "ESQueueActor " must {
    "return empty on emtpy queue" in {
      val queue = system.actorOf(Props(new QueueESDBPopActor))
      queue ! Queue.Pop("stub")
      expectMsg(Queue.Item(Task("stub", Seq())))
    }

    "preserve queue order" in {
      import vkcrawler.Common._
      val popQueue = system.actorOf(Props(new QueueESDBPopActor))
      val pushQueue = system.actorOf(Props(new QueueESDBPushActor))
      val ins = Seq[VKID](1, 2, 3, 4)
      pushQueue ! Queue.Push(ins)
      popQueue ! Queue.Pop("stub")
      expectMsg(Queue.Item(Task("stub", Seq(TaskData(2, None), TaskData(4, None)))))
      popQueue ! Queue.Pop("stub")
      expectMsg(Queue.Item(Task("stub", Seq(TaskData(1, None), TaskData(3, None)))))
      // queue ! Queue.Pop("stub")
      // expectMsg(Queue.Item(Task("stub", Seq(TaskData(2, _), TaskData(4, _)))))
      // queue ! Queue.Pop("stub")
      // expectMsg(Queue.Item(Task("stub", Seq(TaskData(4, None)))))
    }
  }

    // "be idempotent " in {
    //   import vkcrawler.Common._
    //   import ReliableMessaging._
    //   val popSize = QueueMongoDBIntegration.popSize
    //
    //   val queue = system.actorOf(Props(new QueueMongoDBActor{
    //     override def persistenceId = Random.alphanumeric.take(5).mkString
    //   }))
    //
    //   val ins = Seq[VKID](1, 1, 1)
    //   queue ! Queue.Push(ins)
    //   queue ! Queue.Pop("stub2")
    //   //expectMsg(10.seconds, Envelop(2, Queue.Items(1l to popSize)))
    //   expectMsg(
    //     10.seconds,
    //     Envelop(
    //       1,
    //       Queue.Item(
    //         Task("stub2", (1l to popSize).map{id => TaskData(id, None)})
    //       )
    //     )
    //   )
    //   queue ! Queue.Pop("stub2")
    //   expectMsg(
    //     10.seconds,
    //     Envelop(
    //       2,
    //       Queue.Item(
    //         Task("stub2", (1l to popSize).map{id => TaskData(id + popSize, None)})
    //       )
    //     )
    //   )
    // }
  //}
}
