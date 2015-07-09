name := "VKCrawler-0.3-prototype"

version := "0.3.0"

scalaVersion := "2.11.5"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

val akkaVersion = "2.3.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")

assemblySettings

val kamonVersion = "0.3.5"

libraryDependencies ++= Seq(
  "io.kamon" %% "kamon-core" % kamonVersion % "compile",
  "io.kamon" %% "kamon-datadog" % kamonVersion % "compile",
  "io.kamon" %% "kamon-log-reporter" % kamonVersion % "compile",
  "io.kamon" %% "kamon-system-metrics" % kamonVersion % "compile",
  "org.aspectj" % "aspectjweaver" % "1.8.2" % "compile"
)

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.0"

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.0.3" % "test"

aspectjSettings

javaOptions <++= AspectjKeys.weaverOptions in Aspectj

fork := true

javaOptions in run ++= Seq("-Xmx6G")