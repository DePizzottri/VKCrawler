libraryDependencies += "io.spray" %%  "spray-json" % "1.2.6"

libraryDependencies += "org.mongodb" %% "casbah" % "2.8.1"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.0"

libraryDependencies += "io.spray" %% "spray-httpx" % "1.3.1"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "2.4.17" % "test",
  "org.specs2" %%  "specs2-junit" % "2.4.17" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
