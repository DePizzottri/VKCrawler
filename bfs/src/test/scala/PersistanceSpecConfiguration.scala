package vkcrawler.bfs.test

import com.typesafe.config.ConfigFactory

object PersistanceSpecConfiguration {
  def config = ConfigFactory.parseString(
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
