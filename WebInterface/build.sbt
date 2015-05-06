name := """VCCrawler WEB Interface"""

version := "0.0.1"

scalaVersion := "2.11.5"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "io.spray" %% "spray-client" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-can" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-http" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-httpx" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-util" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-routing" % "1.3.1"

libraryDependencies += "io.spray" %%  "spray-json" % "1.2.6"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.2"

libraryDependencies += "org.mongodb" %% "casbah" % "2.8.1"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.0"

scalacOptions ++= Seq("-feature")

Revolver.settings