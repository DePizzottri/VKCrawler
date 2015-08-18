name := """VKCrawler WEB Interface"""

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

val akkaVersion = "2.3.11"

val sprayVersion = "1.3.1"

libraryDependencies += "io.spray" %% "spray-client" % sprayVersion

libraryDependencies += "io.spray" %% "spray-can" % sprayVersion

libraryDependencies += "io.spray" %% "spray-http" % sprayVersion

libraryDependencies += "io.spray" %% "spray-httpx" % sprayVersion

libraryDependencies += "io.spray" %% "spray-util" % sprayVersion

libraryDependencies += "io.spray" %% "spray-routing" % sprayVersion

libraryDependencies += "io.spray" %%  "spray-json" % "1.2.6"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.0"

scalacOptions ++= Seq("-feature")
