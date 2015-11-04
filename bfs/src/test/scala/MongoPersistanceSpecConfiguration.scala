package vkcrawler.bfs.test

import com.typesafe.config.ConfigFactory

object MongoPersistanceSpecConfiguration {
  def config = ConfigFactory.parseString(
    """
    akka.persistence.journal.plugin = "akka-contrib-mongodb-persistence-journal"
    akka.persistence.snapshot-store.plugin = "akka-contrib-mongodb-persistence-snapshot"
    akka.contrib.persistence.mongodb.mongo.mongouri = "mongodb://localhost:27017/test_persistence"

    akka.contrib.persistence.mongodb.mongo.journal-collection = "persistent_journal"
    akka.contrib.persistence.mongodb.mongo.journal-index = "journal_index"
    akka.contrib.persistence.mongodb.mongo.snaps-collection = "persistent_snapshots"
    akka.contrib.persistence.mongodb.mongo.snaps-index = "snaps_index"
    akka.contrib.persistence.mongodb.mongo.journal-write-concern = "Acknowledged"
    """.stripMargin
  )
}
