package bfs.prototype1

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.ActorLogging
import com.typesafe.config.ConfigFactory
import scala.collection._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await
import java.io.PrintWriter
import java.io.File
import akka.actor.TypedActor.PostStop

import akka.routing.RoundRobinPool
import akka.routing._

case class Insert(vs:Seq[Int])
case class Filter(v:Int, vs:Seq[Int])
case class Filtered(v:Int, vs:Seq[Int])

class Used extends Actor {
  var used = mutable.HashSet.empty[Int]
  def receive = {
    case Insert(vs) => used ++= vs
    case Filter(v, vs) => sender ! Filtered(v, vs.filter{x => !used.contains(x)})
  }
}

case class Push(vs: Seq[Int])
case class Pop()
case class Item(v:Int)
case class Empty()

class Queue extends Actor {
  var queue = mutable.Queue.empty[Int]
  def receive = {
    case Push(vs) => queue ++= vs
    case Pop => {
      if(!queue.isEmpty)
        sender ! Item(queue.dequeue())
      else
        sender ! Empty
    }
  }
}

case class GetNeighbours(v:Int)
case class Neighbours(v:Int, vs:Seq[Int])

//generates random graph

class World extends Actor {
  import scala.util.Random
  var m = mutable.HashMap.empty[Int, Seq[Int]]
  def receive = {
    case GetNeighbours(v) => {
      //somehow get incident vertices
      if(!m.contains(v)) {
        m += v -> (1 to 500).map(x => Random.nextInt(1000000)).toSet.toSeq
      }
      sender ! Neighbours(v, m.getOrElse(v, Seq()))
    }
  }
}


case class Save(v:Int, vs:Seq[Int])

//save collected graph
class GraphDB extends Actor with ActorLogging{
  
  val pw = new PrintWriter(new File("graph.txt"))
  
  override def postStop() = {
    pw.flush()
    pw.close()
  }
  
  def receive = {
    case Save(v, vs) => {
      //save graph somewhere
      //log.info(s"$v -> $vs")
      pw.write(s"$v -> $vs\n")
    }
  }
}

case class Start()

class BFS extends Actor with ActorLogging{
  def resolve(name:String):ActorRef = {
  val path = "akka://bfs-system/user/"
    val fa = context.actorSelection(path+name).resolveOne(5 seconds)
    implicit val executionContext = context.system.dispatcher
    Await.result(fa, 6 seconds)
  }
  lazy val queue = resolve("queue")
  lazy val world = resolve("world")
  lazy val graph = resolve("graph")
  lazy val used = resolve("used")
  
  queue ! Push(Seq(1))
  used ! Insert(Seq(1))
  
  def receive = {
    case Start => {
      queue ! Pop
    }
    case Empty => {
      context.system.shutdown()
    }
    case Item(v) => {
      world ! GetNeighbours(v)
    }
    case Neighbours(v, vs) => {
      graph ! Save(v, vs)
      used ! Filter(v, vs)
    }
    case Filtered(v, vs) => {
      used ! Insert(vs)
      queue ! Push(vs)
      self ! Start
    }
  }
}

object Application extends App {
  val config = ConfigFactory.parseString(
      """
//    akka.loglevel = "DEBUG"
//    akka.actor.debug {
//      receive = on
//      lifecycle = on
//    }
    """)  
  
  val system = ActorSystem("bfs-system", config)
  
  val queue = system.actorOf(Props[Queue], "queue")
  val world = system.actorOf(Props[World], "world")
  val graph = system.actorOf(Props[GraphDB], "graph")
  val used = system.actorOf(Props[Used], "used")
  
  val bfs = system.actorOf(RoundRobinPool(3).props(Props[BFS]), "bfs")
  
  bfs ! Broadcast(Start)
}
