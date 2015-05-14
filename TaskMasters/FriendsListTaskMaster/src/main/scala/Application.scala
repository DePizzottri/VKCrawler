package com.vkcrawler.taskMasters.FriendsListTaskMaster

import akka.actor._
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import spray.routing.HttpService
import spray.can._
import com.vkcrawler.ControlService._
import com.vkcrawler.WebControlService._
import akka.event.Logging


object FriendsListRefiner extends App {
  implicit val system = ActorSystem("taskmaster-system")
  
  val commandActor = system.actorOf(Props(classOf[ControlServiceActor], "Friends List TaskMaster Service", FriendsListTaskMasterRoutine.run _), "TaskMasterService")
  val webActor = system.actorOf(Props(classOf[WebControlServiceActor], commandActor), "WebService")
  
  IO (Http) ! Http.Bind(webActor, "0.0.0.0", 8082)  
  
  system.awaitTermination()
}