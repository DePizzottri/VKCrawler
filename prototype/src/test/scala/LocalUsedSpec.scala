package vkcrawler.bfs.prototype3.test

import akka.actor._
import scala.concurrent.duration._

class LocalUsedSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("LocalUsedSpecSystem"))

  import vkcrawler.bfs.prototype3._

  class LocalUsedActor extends UsedActor with LocalUsedBackend

  "LocalUsedActor " must {
    "insert and then filter same item" in {
      val used = system.actorOf(Props(new LocalUsedActor))
      used ! Used.InsertAndFilter(Seq(1))
      expectMsg(Used.Filtered(Seq(1)))
      used ! Used.InsertAndFilter(Seq(1))
      expectMsg(Used.Filtered(Seq()))
    }

    "filter some items" in {
      import Common._
      val used = system.actorOf(Props(new LocalUsedActor))
      val seq1 = Seq[VKID](1, 2, 3, 4)
      val seq2 = Seq[VKID](3, 4, 5)

      used ! Used.InsertAndFilter(seq1)
      expectMsg(Used.Filtered(seq1))

      used ! Used.InsertAndFilter(seq2)
      expectMsg(Used.Filtered(seq2.toSet.diff(seq1.toSet).toSeq))
    }

    "have some throughput" in {
      import Common._
      val used = system.actorOf(Props(new LocalUsedActor))
      val start = System.currentTimeMillis
      val num = 10000
      for (x <- 1 to num) {
        used ! Used.InsertAndFilter((1 to 200).map(x => scala.util.Random.nextInt(1000000).asInstanceOf[VKID]).toSeq)
      }
      receiveN(num, 10.seconds)
      val dur = System.currentTimeMillis - start
      println(s"Local used throughput : ${(num*1.0)/(dur/1000.0)} msg/sec")
    }

  }
}
