name := """VKCrawler WEB Interface"""

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies += "io.spray" %% "spray-client" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-can" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-http" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-httpx" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-util" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-routing" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-testkit" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-json" % "1.2.6"

libraryDependencies += "org.specs2" %% "specs2-core" % "2.4.17" % "test"

libraryDependencies += "org.specs2" %%  "specs2-junit" % "2.4.17" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.2"

libraryDependencies += "org.mongodb" %% "casbah" % "2.8.1"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.0"

libraryDependencies += "com.rabbitmq" % "amqp-client" % "3.5.2"

scalacOptions in Test ++= Seq("-Yrangepos")

scalacOptions ++= Seq("-feature")
