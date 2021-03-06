package vkcrawler.bfs
import com.typesafe.config.{ConfigFactory, Config}

import akka.actor._

abstract class Runner(args:Array[String])
{
  val subsystems = if(args.isEmpty) Array("bfs", "exchange", "used", "graph", "queue") else args

  def name:String

  def run {
    val initActors = Map(
      "bfs" -> bfs,
      "exchange" -> exchange,
      //"graph" -> graph,
      "queue" -> queue,
      "used" -> used
    )

    println(name)
    println("Starting actors")

    for (i <- initActors) {
      if(subsystems.contains(i._1)) {
        println("Start " + i._1)
        i._2
      }
    }

    println("Started")
  }

  def bfs
  def exchange
  def graph
  def queue
  def used
}

class ReliableRunner(args:Array[String], system:ActorSystem, conf:Config) extends Runner(args) {
  override def name = "Reliable BFS"

  override def bfs = {
    system.actorOf(
      Props(
        new ReliableBFSActor(
          ActorPath.fromString(system.toString + conf.getString("bfs.graphactor")),
          ActorPath.fromString(system.toString + conf.getString("bfs.usedactor")),
          ActorPath.fromString(system.toString + conf.getString("bfs.exchangeactor"))
          )
        ),
        "BFSActor"
      )
  }

  override def exchange = {
    class RabbitMQExchangeActor extends ReliableExchangeActor (
      ActorPath.fromString(system.toString + conf.getString("exchange.bfsactor")),
      ActorPath.fromString(system.toString + conf.getString("exchange.queueactor"))
    ) with RabbitMQExchangeBackend

    system.actorOf(Props(new RabbitMQExchangeActor), "ExchangeActor")
  }

  override def graph = {
    class GraphSaverMongoDBActor extends ReliableGraphActor with ReliableMongoDBGraphSaverBackend
    system.actorOf(Props(new GraphSaverMongoDBActor), "GraphActor")
  }

  override def queue = {
    //class QueueMongoDBActor extends ReliableQueueActor with ReliableMongoQueueBackend
    class RichQueueMongoDBActor extends ReliableRichQueueActor {
      class MongoBackendActor extends RichQueueBackendActor with MongoRichQueueBackend
      override def createBackend = new MongoBackendActor
    }
    system.actorOf(Props(new RichQueueMongoDBActor), "QueueActor")
  }

  override def used = {
    class JedisUsedActor extends ReliableUsedActor with JedisUsedBackend
    system.actorOf(Props(new JedisUsedActor), "UsedActor")
  }
}

class UnreliableRunner(args:Array[String], system:ActorSystem, conf:Config) extends Runner(args) {
  override def name = "Unreliable BFS"

  override def bfs = {
    import akka.routing._
    system.actorOf(
      RoundRobinPool(5).props(
      Props(
        new BFSActor(
          ActorPath.fromString(system.toString + conf.getString("bfs.graphactor")),
          ActorPath.fromString(system.toString + conf.getString("bfs.usedactor")),
          ActorPath.fromString(system.toString + conf.getString("bfs.exchangeactor"))
        )
      )),
      "BFSActor"
    )
  }

  override def exchange = {
    class RabbitMQExchangeActor extends ExchangeActor(
      ActorPath.fromString(system.toString + conf.getString("exchange.bfsactor")),
      ActorPath.fromString(system.toString + conf.getString("exchange.queueactor"))
    ) with RabbitMQExchangeBackend

    import akka.routing._
    system.actorOf(RoundRobinPool(5).props(Props(new RabbitMQExchangeActor)), "ExchangeActor")
    //system.actorOf(Props(new RabbitMQExchangeActor), "ExchangeActor")
  }

  override def graph = {
    class GraphSaverMongoDBActor extends GraphActor with ReliableMongoDBGraphSaverBackend {
      override def preStart = {
        init
        super.preStart
      }
    }
    system.actorOf(Props(new GraphSaverMongoDBActor), "GraphActor")
  }

  override def queue = {
    //class QueueMongoDBActor extends QueueActor with MongoQueueBackend
    class QueuePushESDBActor extends QueuePushActor with ESQueueBackend
    class QueuePopESDBActor extends QueuePopActor with ESQueueBackend
    //system.actorOf(Props(new QueueMongoDBActor), "QueueActor")
    import akka.routing._
    system.actorOf(RoundRobinPool(20).props(Props(new QueuePushESDBActor)), "QueueActorPush")
    system.actorOf(RoundRobinPool(10).props(Props(new QueuePopESDBActor)), "QueueActorPop")
  }

  override def used = {
    class JedisUsedActor extends UsedActor with JedisUsedBackend {
      override def preStart = {
        init
        super.preStart
      }
    }
    system.actorOf(Props(new JedisUsedActor), "UsedActor")
  }
}

object Application extends App {
  val conf = ConfigFactory.load();

  val system = ActorSystem("BFSSystem", conf)

  if(args.contains("reliable")) {
    new ReliableRunner(args, system, conf).run
  } else {
    new UnreliableRunner(args, system, conf).run
  }
}
