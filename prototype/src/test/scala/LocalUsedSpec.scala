import akka.actor._
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._
 
class LocalUsedSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("LocalUsedSpecSystem"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
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
      println(s"Throughput : ${(num*1.0)/(dur/1000.0)} msg/sec")
    }

  }
}