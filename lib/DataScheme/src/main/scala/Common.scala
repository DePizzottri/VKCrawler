package vkcrawler

object Common {
  type VKID = Long
}

import akka.actor.ActorSystem
import akka.actor.Actor

class PerfCounter(val name:String, val system: ActorSystem, val actor:Actor, val step:Int = 10) {
  var startTime = 0l
  var allTime = 0l
  var count = 0l
  def begin {
    startTime = System.nanoTime
  }

  def end {
    allTime = allTime + (System.nanoTime - startTime)
    count = count + 1
    if(count % step == 0) {
      akka.event.Logging(system, actor).info(
        "%s timer: %d ms, last: %d ms".format(
          name, 
          (allTime / 1000) / count, 
          (System.nanoTime - startTime)/1000)
      )
    }
  }
}
