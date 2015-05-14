package com.vkcrawler.ControlService

import akka.actor.{Actor, ActorLogging, Cancellable}
import scala.concurrent.duration._
import akka.event.Logging
import akka.event.LoggingAdapter

object ControlServiceActor {
  case class Schedule(val duration: FiniteDuration)
  case class Suspend()
  case class Resume()
  case class Shutdown()
  case class Work()
  case class GetName()
}

class ControlServiceActor(name:String, f: (LoggingAdapter) => Unit) extends Actor with ActorLogging {
  import ControlServiceActor._
  
  def work() = {
    log.info("Working start")
    val start = System.currentTimeMillis()
    f(log)
    val end = System.currentTimeMillis()
    if((end - start) / 1000 > 0)
        log.info(s"Working finished ${(end - start) / 1000}.${(end-start) % 1000}s")
    else
        log.info(s"Working finished ${end-start}ms")
  }

  def stop: Receive = {
    case Shutdown => context.system.shutdown()
    case GetName => sender ! name
  }
  
  def receive = stop orElse {
    case Schedule(d) => context.become(start(d), true)
  }  
  
  def start(duration: FiniteDuration) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val c = context.system.scheduler.schedule(0.seconds, duration)(work)
    working(duration, c)
  }
  
  def working(duration: FiniteDuration, prevWork: Cancellable): Receive = stop orElse {
    case msg@Schedule(d) => prevWork.cancel(); context.become(start(d), true)
    case Suspend => prevWork.cancel(); context.become(suspended(duration) orElse stop)
  }
  
  def suspended(duration: FiniteDuration): Receive = stop orElse {
    case s:Schedule => context.unbecome(); self ! s
    case Resume => context.unbecome(); self ! Schedule(duration)
  }
}
