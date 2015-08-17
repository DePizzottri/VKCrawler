name := """VKCrawler"""

lazy val common = Seq(
  version := "0.3.0",
  scalaVersion := "2.11.5",
  Revolver.settings
)

lazy val root = (project in file(".")).aggregate(DataScheme, WEBInterface, BFS)

lazy val DataScheme = (project in file("lib/DataScheme")).settings(common: _*).settings(assemblySettings)

lazy val BFS = (project in file("BFS")).settings(common: _*).settings(assemblySettings).dependsOn(DataScheme)

lazy val WEBInterface = (project in file("WEBInterface")).settings(common: _*).settings(assemblySettings).dependsOn(DataScheme)

import AssemblyKeys._
