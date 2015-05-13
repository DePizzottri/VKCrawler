package com.vkcrawler.WEBInterface

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

import java.util.Date

import com.vkcrawler.DataModel._
import com.vkcrawler.DataModel.SprayJsonSupport._
import com.vkcrawler.DataModel.SprayJsonSupport.FriendsListTaskResultJsonSupport._
import com.vkcrawler.WEBInterface.MongoDB._

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{Success, Failure}
import scala.language.postfixOps


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
            case _ => detach() {
              complete {
                MongoDBSource.getTask(types.split(","))
              }
            }
          }
        }
      }
    }

  lazy val postTask = {
    path("postTask") {
      post {
        entity(as[FriendsListTaskResult]) { res =>
          MongoDBSource.putTaskResult(res)
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

  val conf = ConfigFactory.load()  
  startServer(interface = conf.getString("WEB.host"), port = conf.getInt("WEB.port")) {
    root ~ hello ~ getTask ~ postTask ~ stop
  }
}
