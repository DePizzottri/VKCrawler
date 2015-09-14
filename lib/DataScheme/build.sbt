val sprayVersion = "1.3.2"

libraryDependencies ++= Seq(
  "io.spray" %% "spray-httpx" % sprayVersion,
  "io.spray" %%  "spray-json" % "1.3.1",
  "org.mongodb" %% "casbah" % "2.8.2",
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "com.typesafe" % "config" % "1.3.0"
)
