package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._

class PersistentUsedSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem("PersistentUsedSpecSystem", PersistanceSpecConfiguration.config))

  import vkcrawler.bfs._

  "PersistantUsedActor " must {
    "insert and then filter same item" in {
      val used = system.actorOf(Props(new PersistentUsedActor))
      used ! Used.InsertAndFilter(Seq(13))
      expectMsg(Used.Filtered(Seq(13)))
      used ! Used.InsertAndFilter(Seq(13))
      expectMsg(Used.Filtered(Seq()))
    }

    "filter some items" in {
      import vkcrawler.Common._
      val used = system.actorOf(Props(new PersistentUsedActor))
      val seq1 = Seq[VKID](1, 2, 3, 4)
      val seq2 = Seq[VKID](3, 4, 5)

      used ! Used.InsertAndFilter(seq1)
      expectMsg(Used.Filtered(seq1))

      used ! Used.InsertAndFilter(seq2)
      expectMsg(Used.Filtered(seq2.toSet.diff(seq1.toSet).toSeq))
    }

    "persist messages after dying" in {
      import vkcrawler.Common._
      val used = system.actorOf(Props(new PersistentUsedActor))

      used ! Used.InsertAndFilter(Seq(33))
      expectMsg(Used.Filtered(Seq(33)))

      used ! PoisonPill

      val used1 = system.actorOf(Props(new PersistentUsedActor))
      used1 ! Used.InsertAndFilter(Seq(33))
      expectMsg(Used.Filtered(Seq()))
    }

    "have some throughput" in {
      import vkcrawler.Common._
      val used = system.actorOf(Props(new PersistentUsedActor))
      val start = System.currentTimeMillis
      val num = 5000
      for (x <- 1 to num) {
        used ! Used.InsertAndFilter((1 to 200).map(x => scala.util.Random.nextInt(1000000).asInstanceOf[VKID]).toSeq)
        //expectMsgClass(1.seconds, classOf[Used.Filtered])
      }
      //more timeout for Travis
      receiveN(num, 30.seconds)
      val dur = System.currentTimeMillis - start
      println(s"Persistant used throughput : ${(num*1.0)/(dur/1000.0)} msg/sec")
    }
  }
}
