name := "AkkaStreamsAgentTest"

scalaVersion := "2.11.7"

import Common._

resolvers ++= Seq (
  Akka.resolver
)

libraryDependencies ++= Seq(
  Akka.stream,
  Akka.httpCore,
  Akka.http,
  Akka.json,
  "joda-time" % "joda-time" % "2.8.2",
  "io.scalac" %% "reactive-rabbit" % "1.0.2"
)

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")
