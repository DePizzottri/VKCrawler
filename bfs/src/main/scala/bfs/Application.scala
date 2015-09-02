package vkcrawler.bfs
import com.typesafe.config.{ConfigFactory, Config}

import akka.actor._

object Application extends App {
  kamon.Kamon.start()

  val params = List(
    ("bfs", "bfs.conf"),
    ("exchange", "exchange.conf"),
    ("graph", "graph.conf"),
    ("queue", "queue.conf"),
    ("used", "used.conf")
  )

  def loadConf(args:Array[String]) = {
    def load(params:List[(String, String)]):Config = {
      if(params.isEmpty)
        ConfigFactory.load()
      else {
        if(args.contains(params.head._1))
        {
          println(params.head._2 + " loaded")
          ConfigFactory.load(params.head._2).withFallback(load(params.tail))
        }
        else
          ConfigFactory.load.withFallback(load(params.tail))
      }
    }

    load(params)
  }

  val arr = if(args.isEmpty) Array("bfs", "exchange", "used", "graph", "queue") else args

  val conf = ConfigFactory.load().withFallback(loadConf(arr))

  val system = ActorSystem("BFSSystem", conf)

  if(system.settings.config.getString("akka.remote.asdasd") != "asd")
    throw new Exception("Config not loaded!")

  val initActors = Map(
    "bfs" -> bfs,
    "exchange" -> exchange,
    "graph" -> graph,
    "queue" -> queue,
    "used" -> used
  )

  for (i <- initActors) {
    if(arr.contains(i._1)) {
      println("Start " + i._1)
      i._2
    }
  }

  println("Started")

  def bfs = system.actorOf(
      Props(
        new ReliableBFSActor(
          ActorPath.fromString(system.toString + conf.getString("bfs.graphactor")),
          ActorPath.fromString(system.toString + conf.getString("bfs.usedactor")),
          ActorPath.fromString(system.toString + conf.getString("bfs.exchangeactor"))
          )
        ),
        "BFSActor"
      )

  class RabbitMQExchangeActor extends ReliableExchangeActor(
    ActorPath.fromString(system.toString + conf.getString("exchange.bfsactor")),
    ActorPath.fromString(system.toString + conf.getString("exchange.queueactor"))
  ) with RabbitMQExchangeBackend

  def exchange = system.actorOf(Props(new RabbitMQExchangeActor), "ExchangeActor")

  class GraphSaverMongoDBActor extends ReliableGraphActor with ReliableMongoDBGraphSaverBackend
  def graph = system.actorOf(Props(new GraphSaverMongoDBActor), "GraphActor")

  class QueueMongoDBActor extends ReliableQueueActor with ReliableMongoQueueBackend
  def queue = system.actorOf(Props(new QueueMongoDBActor), "QueueActor")

  class JedisUsedActor extends ReliableUsedActor with JedisUsedBackend
  def used = system.actorOf(Props(new JedisUsedActor), "UsedActor")
}
