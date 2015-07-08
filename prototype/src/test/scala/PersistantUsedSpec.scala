import akka.actor._
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory


object PersistentUsedSpec{
  def config = ConfigFactory.parseString(
/*
    """ 
    akka.persistence.journal.leveldb.dir = "target/example/journal"
    akka.persistence.snapshot-store.local.dir = "target/example/snapshots"
    """
*/

    """
    akka {
      persistence {
        journal.plugin = "inmemory-journal"
        snapshot-store.plugin = "inmemory-snapshot-store"
      }
    }
    """.stripMargin

  )
}
 
class PersistentUsedSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("PersistentUsedSpecSystem", PersistentUsedSpec.config))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  import vkcrawler.bfs.prototype3._

  "PersistantUsedActor " must {
    "insert and then filter same item" in {
      val used = system.actorOf(Props(new PersistentUsedActor))
      used ! Used.InsertAndFilter(Seq(13))
      expectMsg(Used.Filtered(Seq(13)))
      used ! Used.InsertAndFilter(Seq(13))
      expectMsg(Used.Filtered(Seq()))
    }
 
    "filter some items" in {
      import Common._
      val used = system.actorOf(Props(new PersistentUsedActor))
      val seq1 = Seq[VKID](1, 2, 3, 4)
      val seq2 = Seq[VKID](3, 4, 5)

      used ! Used.InsertAndFilter(seq1)
      expectMsg(Used.Filtered(seq1))

      used ! Used.InsertAndFilter(seq2)
      expectMsg(Used.Filtered(seq2.toSet.diff(seq1.toSet).toSeq))
    }

    "persist messages after dying" in {
      import Common._
      val used = system.actorOf(Props(new PersistentUsedActor))

      used ! Used.InsertAndFilter(Seq(33))
      expectMsg(Used.Filtered(Seq(33)))
      
      used ! PoisonPill

      val used1 = system.actorOf(Props(new PersistentUsedActor))
      used1 ! Used.InsertAndFilter(Seq(33))
      expectMsg(Used.Filtered(Seq()))
    }

    "have some throughput" in {
      import Common._
      val used = system.actorOf(Props(new PersistentUsedActor))
      val start = System.currentTimeMillis
      val num = 10000
      for (x <- 1 to num) {
        used ! Used.InsertAndFilter((1 to 200).map(x => scala.util.Random.nextInt(1000000).asInstanceOf[VKID]).toSeq)
        //expectMsgClass(1.seconds, classOf[Used.Filtered])
      }
      receiveN(num, 10.seconds)
      val dur = System.currentTimeMillis - start
      println(s"Throughput : ${(num*1.0)/(dur/1000.0)} msg/sec")
    }
  }
}