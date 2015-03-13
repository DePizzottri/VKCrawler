package com.vkcrawler.WEBInterface

import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp
import spray.routing.ValidationRejection
import spray.json._
import spray.http.StatusCodes
import java.util.Date
import com.vkcrawler.DataModel._
import com.vkcrawler.DataModel.SprayJsonSupport._
import com.vkcrawler.DataModel.SprayJsonSupport.FriendsListTaskResultJsonSupport._

import com.vkcrawler.WEBInterface.MongoDB._

object Application extends App with SimpleRoutingApp {

  implicit val system = ActorSystem("CrawlerWEBInterface-system")

  lazy val root =
    path("") {
      get {
        redirect("hello", StatusCodes.TemporaryRedirect)
      }
    }

  import TaskJsonSupport._

  lazy val getTask =
    path("getTask") {
      get {
        parameters("version".as[Int], "types".as[String]) { (version, types) =>
          types match {
            case "" => reject(ValidationRejection("""Parameter "types" can not be empty"""))
            case _ => complete {
              MongoDBSource.getTask(types.split(","))
            }
          }
        }
      }
    }

  lazy val putTask = {
    path("putTask") {
      put {
        entity(as[FriendsListTaskResult]) { res =>
          complete("Ok (not implemented)")
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

  startServer(interface = "localhost", port = 8080) {
    root ~ hello ~ getTask ~ putTask
  }
}