package com.vkcrawler.WebControlService

import akka.actor._

import scala.concurrent.duration._
import scala.language.postfixOps

import spray.routing.HttpService
import spray.can._

import akka.pattern.{ask, pipe}
import akka.util.Timeout


class WebControlServiceActor(val service: ActorRef) extends Actor with HttpService {
  def actorRefFactory = context
  def receive = runRoute(route)
  
  import com.vkcrawler.ControlService.ControlServiceActor._

  val route = {
    path("") {
      get {
        complete("Service web interface")
      }
    } ~    
    path("suspend") {
      get { 
        complete {
          service ! Suspend
          "OK"
        }
      }
    } ~ 
    path("resume") {
      get {
        complete {
          service ! Resume
          "OK"
        }
      }
    } ~
    path("shutdown") {
      get {
        complete {
          service ! Shutdown
          "OK"
        }
      }
    } ~ 
    path("schedule") {
      get {
        parameters("duration".as[Int]) { duration =>
          complete {
            service ! Schedule(duration.seconds)
            "OK"
          }
        }
      }
    } ~
    path("name") {
      get {
        implicit val timeout = Timeout(5 seconds)
        val f = (service ? GetName).mapTo[String]
        import scala.concurrent.ExecutionContext.Implicits.global
        onSuccess(f) { name =>
          complete(name)
        }
      }
    }
  }
}
