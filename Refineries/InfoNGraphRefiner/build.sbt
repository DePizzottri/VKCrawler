name := """InfoNGraphRefiner"""

import Common._

libraryDependencies ++= Seq(
  Other.casbah,
  Other.slf4j,
  Other.rabbitmq
)

scalacOptions ++= Seq("-feature")
