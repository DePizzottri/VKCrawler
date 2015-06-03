name := """VKCrawler WEB Interface"""

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"


//spray 
val sprayVersion = "1.3.1"
libraryDependencies ++= Seq(
  "io.spray" %% "spray-client" % sprayVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-http" % sprayVersion,
  "io.spray" %% "spray-httpx" % sprayVersion,
  "io.spray" %% "spray-util" % sprayVersion,
  "io.spray" %% "spray-routing" % sprayVersion,
  "io.spray" %% "spray-testkit" % sprayVersion,
  "io.spray" %% "spray-json" % "1.2.6"
)

//test
libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "2.4.17" % "test",
  "org.specs2" %%  "specs2-junit" % "2.4.17" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

//other
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.2"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.0"

libraryDependencies += "org.mongodb" %% "casbah" % "2.8.1"

libraryDependencies += "com.rabbitmq" % "amqp-client" % "3.5.2"

scalacOptions in Test ++= Seq("-Yrangepos")

scalacOptions ++= Seq("-feature")
