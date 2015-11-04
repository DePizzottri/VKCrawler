package vkcrawler.bfs.test

import akka.actor._
import scala.concurrent.duration._
import scala.util.Random
import com.typesafe.config.ConfigFactory
import redis.clients.jedis._

object JedisUsedSpec {
  def getRandomString = Random.alphanumeric.take(5).mkString

  def config = ConfigFactory.parseString(
    """
    used {
      redis {
        host = localhost
        port = 6379
        setName = "__test_used"
        timeout = 10
      }
    }
    """.stripMargin
  )
}

class JedisUsedSpec(_system: ActorSystem) extends BFSTestSpec(_system) {

  def this() = this(ActorSystem(
    "JedisUsedSpecSystem",
    JedisUsedSpec.config.withFallback(PersistanceSpecConfiguration.config)))

  def cleanRedis = {
    //clean redis
    val conf = system.settings.config
    val jedis = new Jedis(conf.getString("used.redis.host"), conf.getInt("used.redis.port"))
    val uidsSet = conf.getString("used.redis.setName")
    jedis.del(uidsSet)
  }

  override def beforeAll = {
    cleanRedis
  }

  override def afterAll = {
    cleanRedis
    system.shutdown()
  }

  import vkcrawler.bfs._

  class JedisUsedActor extends ReliableUsedActor with JedisUsedBackend

  class JedisUsedRandActor extends JedisUsedActor {
    override val persistenceId = "jedis_used"+ JedisUsedSpec.getRandomString
  }

  "JedisUsedActor " must {
    "insert and then filter same item" in {
      import ReliableMessaging._

      val used = system.actorOf(Props(new JedisUsedRandActor))
      used ! Used.InsertAndFilter(Seq(13))
      val e1 = expectMsgClass(classOf[Envelop])
      e1.msg should be (Used.Filtered(Seq(13)))
      used ! Confirm(e1.deliveryId)

      used ! Used.InsertAndFilter(Seq(13))
      val e2 = expectMsgClass(classOf[Envelop])
      e2.msg should be (Used.Filtered(Seq()))
      used ! Confirm(e2.deliveryId)
    }

    "filter some items" in {
      import vkcrawler.Common._
      import ReliableMessaging._

      val used = system.actorOf(Props(new JedisUsedRandActor))
      val seq1 = Seq[VKID](1, 2, 3, 4)
      val seq2 = Seq[VKID](3, 4, 5)

      used ! Used.InsertAndFilter(seq1)
      val e1 = expectMsgClass(classOf[Envelop])
      e1.msg should be (Used.Filtered(seq1))
      used ! Confirm(e1.deliveryId)

      used ! Used.InsertAndFilter(seq2)
      val e2 = expectMsgClass(classOf[Envelop])
      e2.msg should be (Used.Filtered(seq2.toSet.diff(seq1.toSet).toSeq))
      used ! Confirm(e2.deliveryId)
    }

    "redeliver messages after dying" in {
      import vkcrawler.Common._
      import ReliableMessaging._

      val used = system.actorOf(Props(new JedisUsedActor))

      used ! Used.InsertAndFilter(Seq(33))
      val e1 = expectMsgClass(classOf[Envelop])
      e1.msg should be (Used.Filtered(Seq(33)))

      used ! PoisonPill

      val used1 = system.actorOf(Props(new JedisUsedActor))
      val e2 = expectMsgClass(classOf[Envelop])
      e2.msg should be (Used.Filtered(Seq(33)))
      used ! Confirm(e1.deliveryId)
    }

    "have some throughput" in {
      import vkcrawler.Common._
      import ReliableMessaging._

      val used = system.actorOf(Props(new JedisUsedRandActor))
      val start = System.currentTimeMillis
      val num = 10000
      for (x <- 1 to num) {
        used ! Used.InsertAndFilter((1 to 200).map(x => scala.util.Random.nextInt(1000000).asInstanceOf[VKID]).toSeq)
      }
      val ans = receiveN(num, 30.seconds)
      val dur = System.currentTimeMillis - start
      println(s"Jedis used throughput : ${(num*1.0)/(dur/1000.0)} msg/sec")
      ans.foreach{x => used ! Confirm(x.asInstanceOf[Envelop].deliveryId)}
    }
  }
}
