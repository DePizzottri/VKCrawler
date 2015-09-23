name := """WallPosts"""

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

val sprayVersion = "1.3.2"

val akkaVersion = "2.3.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-http" % sprayVersion,
  "io.spray" %% "spray-routing" % sprayVersion,
  "org.mongodb" %% "casbah" % "2.8.2",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "com.rabbitmq" % "amqp-client" % "3.5.4"
)

scalacOptions ++= Seq("-feature")
