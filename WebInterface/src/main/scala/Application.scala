package vkcrawler.WEBInterface

import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import spray.can.Http
import spray.routing.SimpleRoutingApp
import spray.routing.ValidationRejection
import spray.json._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._

import java.util.Date

import vkcrawler.DataModel._
import vkcrawler.DataModel.SprayJsonSupport._
import TaskResultJsonSupport._
import FriendsListJsonSupport._

// import vkcrawler.DataModel.SprayJsonSupport.TaskResultJsonSupport._

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.language.postfixOps

object Application extends App with SimpleRoutingApp {
  //kamon.Kamon.start()

  implicit val system = ActorSystem("CrawlerWEBInterface-system")

  //connect ro rabbit

  val conf = ConfigFactory.load()

  val queue = system.actorSelection(conf.getString("queueactor"))
  val exchange = system.actorSelection(conf.getString("exchangeactor"))

  lazy val root =
    path("") {
      get {
        redirect("hello", StatusCodes.TemporaryRedirect)
      }
    }

  import TaskJsonSupport._

  val isReliableBFS = args.contains("reliable")

  lazy val getTask =
    path("getTask") {
      get {
        parameters("version".as[Int], "types".as[String]) { (version, types) =>
          types match {
            case "" => reject(ValidationRejection("""Parameter "types" can not be empty"""))
            case _ => detach() {
              //MongoDBSource.getTask(types.split(","))
              import vkcrawler.bfs._
              import scala.concurrent.ExecutionContext.Implicits.global
              implicit val timeout = Timeout(15 seconds)
              if(isReliableBFS)
              {
                val f = (queue ? RichQueue.PopUnreliable(types.split(","))).mapTo[RichQueue.Item].map{
                  case msg@RichQueue.Item(t) => t
                      //case msg@Queue.Empty => """{error:"No task"}""".parseJson
                }
                import spray.httpx.SprayJsonSupport._
                complete(f)
              }
              else
              {
                import scala.util.Random
                if(types.split(",").size > 0) {
                  val `type` = Random.shuffle(types.split(",").toList).head

                  val f = (queue ? Queue.Pop(`type`)).mapTo[Queue.Item].map{
                     case Queue.Item(task) => task
                  }
                  import spray.httpx.SprayJsonSupport._
                  complete(f)
                }
                else
                {
                  complete("""{"error":"bad type field in query"}""")
                }
              }
            }
          }
        }
      }
    }

  val filterCity = conf.getInt("WEB.filterCity")
  val noFilterCity = filterCity == -1

  lazy val postTask = {
    path("postTask") {
      post {
        entity(as[TaskResult]) { res =>
          res.`type` match {
            case "friends_list" => detach(){
              val friends = res.data.convertTo[Seq[FriendsList]]
              for(info <- friends) {
                if(noFilterCity)
                  exchange ! vkcrawler.bfs.BFS.Friends(info.uid, info.friends.map(x => x.uid))
                else
                  exchange ! vkcrawler.bfs.BFS.Friends(info.uid, info.friends.filter(x => x.city == filterCity).map(x => x.uid))
              }

              complete("Ok")
            }
            case "user_info" => detach(){
              val infos = res.data.convertTo[Seq[JsValue]]
              for(info <- infos) {
                exchange ! vkcrawler.bfs.Exchange.Publish("user_info", info)
              }
              complete("Ok")
            }
            case "wall_posts" => detach(){
              for(p <- res.data.convertTo[Seq[JsValue]]) {
                exchange ! vkcrawler.bfs.Exchange.Publish("wall_posts", p)
              }

              complete("Ok")
            }
          }
        }
      }
    }
  }

  lazy val hello =
    path("hello") {
      get {
        complete {
          <h1>Welcome to VC Crawler Task frontend</h1>
        }
      }
    }

  lazy val stop = {
    path("shutdown") {
      get {
        complete {
          //graceful stop
          implicit val timeout = Timeout(15 seconds)
          implicit val executionContext = system.dispatcher
          Await.result(IO(Http) ? Http.CloseAll, 15 seconds)
          system.stop(IO(Http))
          system.terminate();
          "Ok"
        }
      }
    }
  }

  //val conf = ConfigFactory.load()
  startServer(interface = conf.getString("WEB.host"), port = conf.getInt("WEB.port")) {
    root ~ hello ~ getTask ~ postTask ~ stop
  }
}
