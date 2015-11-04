name := """WallPosts"""

resolvers ++= Seq (
  Spray.resolver
)

import Common._

libraryDependencies ++= Seq(
  Spray.http,
  Spray.routing,
  Other.casbah,
  Other.slf4j,
  Other.rabbitmq
)

scalacOptions ++= Seq("-feature")
