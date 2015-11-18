import sbt._

object Common {

    // object Other {
    //   val jedis = "redis.clients" % "jedis" % "2.7.2"
    //   val rabbitmq = "com.rabbitmq" % "amqp-client" % "3.5.4"
    //   val casbah = "org.mongodb" %% "casbah" % "2.8.2"
    //
    //   val slf4j = "org.slf4j" % "slf4j-simple" % "1.7.7"
    //
    //   val scalaTest = "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    //
    //   val typesafeConfig = "com.typesafe" % "config" % "1.3.0"
    // }

    object Akka {
      val resolver = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
      val groupId = "com.typesafe.akka"
      val version = "2.3.11"
      val actor =   groupId %% "akka-actor"                         % version
      val cluster = groupId %% "akka-cluster"                       % version
      val ddata =   groupId %% "akka-distributed-data-experimental" % version

      val streamVersion = "2.0-M1"
      val stream =    groupId %% "akka-stream-experimental"          % streamVersion
      val httpCore =  groupId %% "akka-http-core-experimental"       % streamVersion
      val http =      groupId %% "akka-http-experimental"            % streamVersion
      val json =      groupId %% "akka-http-spray-json-experimental" % streamVersion
    }

    // object Spray {
    //   val resolver = "spray repo" at "http://repo.spray.io"
    //   val groupId = "io.spray"
    //   val version = "1.3.2"
    //   val client = groupId %% "spray-client" % version
    //   val can = groupId %% "spray-can" % version
    //   val http = groupId %% "spray-http" % version
    //   val httpx = groupId %% "spray-httpx" % version
    //   val util = groupId %% "spray-util" % version
    //   val routing = groupId %% "spray-routing" % version
    //   //val testkit = groupId %% "spray-testkit" % version,
    //   val json = groupId %% "spray-json" % version
    // }

    // object Kamon {
    //   val groupId = "io.kamon"
    //   val version = "0.5.1"
    //   val core = groupId %% "kamon-core" % version
    //   val akka = groupId %% "kamon-akka" % version
    //   val akka_remote = groupId %% "kamon-akka-remote" % version
    //   val statsd = groupId %% "kamon-statsd" % version
    //   val metrics = groupId %% "kamon-system-metrics" % version
    //   val spray = groupId %% "kamon-spray" % version
    //   val aspectj = "org.aspectj" % "aspectjweaver" % "1.8.6"
    // }
    //
    // object Plugins {
    //   val revolver = "io.spray" % "sbt-revolver" % "0.7.2"
    //   val assembly = "com.eed3si9n" % "sbt-assembly" % "0.13.0"
    //   val aspectj = "com.typesafe.sbt" % "sbt-aspectj" % "0.10.3"
    // }
}
