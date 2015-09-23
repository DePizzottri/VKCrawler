name := """VKCrawler"""

lazy val common = Seq(
  version := "0.3.0",
  scalaVersion := "2.11.7",
  Revolver.settings
)

lazy val WEBInterface = (project in file("WEBInterface")).settings(common: _*).dependsOn(DataScheme, BFS)

lazy val root = (project in file(".")).aggregate(DataScheme, WEBInterface, BFS, InfoNGraphRefiner)

lazy val DataScheme = (project in file("lib/DataScheme")).settings(common: _*)

lazy val BFS = (project in file("BFS")).settings(common: _*).dependsOn(DataScheme)

lazy val InfoNGraphRefiner = (project in file("Refineries/InfoNGraphRefiner")).settings(common: _*).dependsOn(DataScheme)