package com.vkcrawler.WEBInterface

import akka.actor.ActorSystem
import akka.io.IO
import com.typesafe.config.ConfigFactory
import akka.routing.FromConfig
import spray.can.Http
import java.net.InetSocketAddress
import akka.actor.Props

object Application extends App {
  implicit val system = ActorSystem("CrawlerWEBInterface-system")
  
  val router = system.actorOf(FromConfig.props(Props[ConnectionHandlerActor]), "router")
  
  //val router = system.actorOf(Props[ConnectionHandlerActor], "handler")
  
  val conf = ConfigFactory.load()
  
  akka.io.IO (Http) ! Http.Bind(router, conf.getString("WEB.host"), conf.getInt("WEB.port"))
}
