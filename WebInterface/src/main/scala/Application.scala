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
import vkcrawler.DataModel.SprayJsonSupport.FriendsListTaskResultJsonSupport._

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.language.postfixOps

object Application extends App with SimpleRoutingApp {
  kamon.Kamon.start()

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

  import FriendsListTaskJsonSupport._

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
              val f = (queue ? Queue.Pop).mapTo[ReliableMessaging.Envelop].map{
                envlp =>
                  queue ! ReliableMessaging.Confirm(envlp.deliveryId)
                  envlp.msg match {
                    case msg@Queue.Items(ids) => FriendsListTask("friends_list", ids)
                    //case msg@Queue.Empty => """{error:"No task"}""".parseJson
                  }
              }
              import spray.httpx.SprayJsonSupport._
              complete(f)
            }
          }
        }
      }
    }

  lazy val postTask = {
    path("postTask") {
      post {
        entity(as[FriendsListTaskResult]) { res =>
          for(info <- res.friends) {
            import UserInfoJsonSupport._
            exchange ! vkcrawler.bfs.BFS.Friends(info.uid, info.friends.filter(x => x.city == 57).map(x => x.uid))
            exchange ! vkcrawler.bfs.Exchange.Publish("user_info", info.toJson.compactPrint)
          }

          complete("Ok")
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
          system.shutdown();
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
