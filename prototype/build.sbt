name := "VKCrawler-0.3-prototype"

version := "0.3.0"

scalaVersion := "2.11.5"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-remote" % "2.3.11"
)

scalacOptions ++= Seq("-feature", "-deprecation")

assemblySettings
