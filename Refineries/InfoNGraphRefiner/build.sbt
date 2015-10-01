name := """InfoNGraphRefiner"""

import Common._

resolvers ++= Seq (
  Akka.resolver
)

libraryDependencies ++= Seq(
  Akka.actor,
  Other.casbah,
  Other.slf4j,
  Other.rabbitmq
)

scalacOptions ++= Seq("-feature")
