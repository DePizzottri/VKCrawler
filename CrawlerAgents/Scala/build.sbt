name := """Crawler agent"""

version := "0.0.1"

scalaVersion := "2.11.2"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.6"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.0"

libraryDependencies += "org.mongodb" %% "casbah" % "2.7.3"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23"
)