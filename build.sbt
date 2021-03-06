name := """VKCrawler"""

lazy val common = Seq(
  version := "0.3.0",
  scalaVersion := "2.11.7"
)

lazy val root = (project in file(".")).aggregate(DataScheme, WEBInterface, BFS)

lazy val DataScheme = (project in file("lib/DataScheme")).settings(common: _*)

lazy val WEBInterface = (project in file("WebInterface")).settings(common: _*).dependsOn(DataScheme, BFS)

lazy val BFS = (project in file("bfs")).settings(common: _*).dependsOn(DataScheme)
