package vkcrawler.bfs.test

import akka.actor.ActorSystem
import akka.testkit.{TestActors, TestKit, ImplicitSender}
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Assertions._

class BFSTestSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {
    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }
  }
