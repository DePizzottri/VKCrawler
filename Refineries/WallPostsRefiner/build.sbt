name := """WallPosts"""

resolvers ++= Seq (
  Akka.resolver,
  Spray.resolver
)

import Common._

libraryDependencies ++= Seq(
  Akka.actor,
  Spray.can,
  Spray.http,
  Spray.routing,
  Other.casbah,
  Other.slf4j,
  Other.rabbitmq
)

scalacOptions ++= Seq("-feature")
